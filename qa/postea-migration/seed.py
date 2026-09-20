#!/usr/bin/env python3
"""Seeds and asserts the Postea migration gate on a pack-server world; driven by run.sh.

Subcommands (all take the world directory as the first argument):
  discover              print the material list version, the witness material's index, and the
                        materiallib item names in the world's saved id map
  seed <itemName>       write old-version witness stacks into EnderStorage, a Backpack file, an
                        OpenBlocks death dump, the GT linked-input-bus table, a player tag, and
                        the quest database; records expectations in <world>/postea-qa-expect.json
  assert-shift          after the examples-on boot: the store advanced one version and the quest
                        database's witness entries were remapped and stamped
  assert-idempotent     after an unchanged boot: nothing moved again
"""
import gzip, io, json, struct, sys, os

BLOCK_PREFIX = chr(1)
ITEM_PREFIX = chr(2)

# ---------- minimal NBT ----------

def _ws(s):
    b = s.encode('utf8'); return struct.pack('>H', len(b)) + b

def t_byte(v): return (1, struct.pack('>b', v))
def t_short(v): return (2, struct.pack('>h', v))
def t_int(v): return (3, struct.pack('>i', v))
def t_long(v): return (4, struct.pack('>q', v))
def t_float(v): return (5, struct.pack('>f', v))
def t_double(v): return (6, struct.pack('>d', v))
def t_str(v): return (8, _ws(v))

def t_list(elem_type, payloads):
    return (9, bytes([elem_type]) + struct.pack('>i', len(payloads)) + b''.join(payloads))

def t_comp(pairs):
    out = b''
    for name, (t, payload) in pairs:
        out += bytes([t]) + _ws(name) + payload
    return (10, out + b'\x00')

def nbt_file(root_pairs):
    t, payload = t_comp(root_pairs)
    return gzip.compress(bytes([t]) + _ws('') + payload)

def read_nbt(buf):
    f = io.BytesIO(buf)
    def rname():
        n = struct.unpack('>H', f.read(2))[0]; return f.read(n).decode('utf8', 'replace')
    def pay(t):
        if t == 0: return None
        if t == 1: return struct.unpack('>b', f.read(1))[0]
        if t == 2: return struct.unpack('>h', f.read(2))[0]
        if t == 3: return struct.unpack('>i', f.read(4))[0]
        if t == 4: return struct.unpack('>q', f.read(8))[0]
        if t == 5: return struct.unpack('>f', f.read(4))[0]
        if t == 6: return struct.unpack('>d', f.read(8))[0]
        if t == 7:
            n = struct.unpack('>i', f.read(4))[0]; return f.read(n)
        if t == 8:
            n = struct.unpack('>H', f.read(2))[0]; return f.read(n).decode('utf8', 'replace')
        if t == 9:
            et = f.read(1)[0]; n = struct.unpack('>i', f.read(4))[0]
            return [pay(et) for _ in range(n)]
        if t == 10:
            out = {}
            while True:
                ct = f.read(1)[0]
                if ct == 0: return out
                key = rname()
                out[key] = pay(ct)
        if t == 11:
            n = struct.unpack('>i', f.read(4))[0]
            return list(struct.unpack('>%di' % n, f.read(4 * n)))
        raise ValueError('tag %d' % t)
    t = f.read(1)[0]; rname()
    return pay(t)

# ---------- helpers ----------

WITNESS = os.environ.get('POSTEA_QA_MATERIAL', 'Tin')
UUID = '11111111-2222-3333-4444-555555555555'

def expect_file(world):
    return os.path.join(world, 'postea-qa-expect.json')

def material_store(world):
    with open(os.path.join(world, 'materiallib', 'material-ids.json'), encoding='utf8') as fh:
        return json.load(fh)

def item_id(world, name):
    data = read_nbt(gzip.decompress(open(os.path.join(world, 'level.dat'), 'rb').read()))
    for entry in data['FML']['ItemData']:
        if entry['K'] == ITEM_PREFIX + name:
            return entry['V']
    raise SystemExit('item %s not in the world id map' % name)

def stack(item, damage, slot_tag):
    pairs = [slot_tag, ('Count', t_byte(1)), ('Damage', t_short(damage))]
    if isinstance(item, str):
        pairs.insert(1, ('id', t_str(item)))
    elif item < 32000:
        pairs.insert(1, ('id', t_short(item)))
    else:
        pairs.insert(1, ('id', t_short(0)))
        pairs.insert(2, ('idExt', t_int(item)))
    return t_comp(pairs)

def quest_db(world):
    return os.path.join(world, 'betterquesting', 'QuestDatabase.json')

def witness_damages(db, item):
    # Every stack of the witness item, wherever the quest sits: dreamcraft reimports the default database
    # whenever it decides the modpack changed, discarding seeded entries and quest order.
    out = []
    def walk(node):
        if isinstance(node, dict):
            if node.get('id:8') == item and 'Damage:2' in node:
                out.append(node['Damage:2'])
            for v in node.values():
                walk(v)
    walk(db.get('questDatabase:9', {}))
    return out

# ---------- subcommands ----------

def discover(world):
    store = material_store(world)
    print('listVersion', store['listVersion'])
    print(WITNESS, 'index', store['materials'][WITNESS])
    data = read_nbt(gzip.decompress(open(os.path.join(world, 'level.dat'), 'rb').read()))
    for entry in data['FML']['ItemData']:
        if entry['K'].startswith(ITEM_PREFIX) and 'materiallib' in entry['K']:
            print(entry['K'][1:], entry['V'])

def seed(world, item_name):
    store = material_store(world)
    if store['listVersion'] != 1:
        raise SystemExit('expected a fresh v1 store, found listVersion %d' % store['listVersion'])
    old_index = store['materials'][WITNESS]
    numeric = item_id(world, item_name)

    es_dir = os.path.join(world, 'EnderStorage')
    os.makedirs(es_dir, exist_ok=True)
    open(os.path.join(es_dir, 'lock.dat'), 'wb').write(b'\x00')
    chest = t_comp([
        ('Items', t_list(10, [stack(numeric, old_index, ('Slot', t_short(0)))[1]])),
        ('size', t_byte(1))])
    open(os.path.join(es_dir, 'data1.dat'), 'wb').write(nbt_file([('0|global|item', chest)]))

    bp_dir = os.path.join(world, 'backpacks', 'backpacks')
    os.makedirs(bp_dir, exist_ok=True)
    open(os.path.join(bp_dir, UUID + '.dat'), 'wb').write(
        nbt_file([('Items', t_list(10, [stack(numeric, old_index, ('Slot', t_byte(0)))[1]]))]))

    dump = [
        ('Type', t_str('death')),
        ('PlayerName', t_str('qa-postea')),
        ('Created', t_long(0)),
        ('PlayerUUID', t_str(UUID)),
        ('Inventory', t_comp([
            ('size', t_int(1)),
            ('Items', t_list(10, [stack(item_name, old_index, ('Slot', t_byte(0)))[1]]))]))]
    open(os.path.join(world, 'data', 'inventory-qa-postea-death-0.dat'), 'wb').write(nbt_file(dump))

    channel = t_comp([('0', stack(numeric, old_index, ('Slot', t_byte(0)))), ('ref', t_int(1))])
    open(os.path.join(world, 'data', 'LinkedInputBusses.dat'), 'wb').write(
        nbt_file([('data', t_comp([('qa', channel)]))]))

    zeros3 = t_list(6, [t_double(0.0)[1]] * 3)
    player = [
        ('Pos', zeros3),
        ('Motion', zeros3),
        ('Rotation', t_list(5, [t_float(0.0)[1]] * 2)),
        ('Inventory', t_list(10, [stack(numeric, old_index, ('Slot', t_byte(0)))[1]]))]
    open(os.path.join(world, 'qa-postea-player.dat'), 'wb').write(nbt_file(player))

    with open(quest_db(world), encoding='utf8') as fh:
        db = json.load(fh)
    damages = witness_damages(db, item_name)
    if not damages:
        raise SystemExit('no %s entries in the quest database to witness' % item_name)
    json.dump(
        {'item': item_name, 'numericId': numeric, 'oldIndex': old_index},
        open(expect_file(world), 'w'),
        indent=2)
    print('seeded: %s (%s) id=%d oldIndex=%d; %d quest entries witness the shift'
          % (WITNESS, item_name, numeric, old_index, len(damages)))

def assert_shift(world):
    exp = json.load(open(expect_file(world)))
    store = material_store(world)
    assert store['listVersion'] == 2, 'listVersion %d, expected 2' % store['listVersion']
    new_index = store['materials'][WITNESS]
    assert new_index != exp['oldIndex'], 'witness index did not move; pick a later-sorting material'
    trans = os.path.join(world, 'materiallib', 'transitions', 'v1-to-v2.json')
    assert os.path.isfile(trans), 'transition v1-to-v2.json missing'
    db = json.load(open(quest_db(world), encoding='utf8'))
    damages = witness_damages(db, exp['item'])
    assert damages, 'no %s entries in the quest database' % exp['item']
    assert new_index in damages, \
        'no quest entry holds the witness at its new index %d (old %d); found %r' \
        % (new_index, exp['oldIndex'], sorted(set(damages))[:10])
    assert exp['oldIndex'] not in damages, \
        'a quest entry still holds the old index %d: the remap missed it' % exp['oldIndex']
    stamps = db.get('POSTEA_VERSIONS:10', {})
    assert stamps.get('materiallib:idList:3') == 2, 'quest DB stamp: %r' % stamps
    exp['newIndex'] = new_index
    exp['witnessCount'] = damages.count(new_index)
    json.dump(exp, open(expect_file(world), 'w'), indent=2)
    print('shift verified: %d quest entries at new index %d, none at old %d, DB stamped v2'
          % (exp['witnessCount'], new_index, exp['oldIndex']))

def assert_idempotent(world):
    exp = json.load(open(expect_file(world)))
    store = material_store(world)
    assert store['listVersion'] == 2, 'listVersion %d, expected 2' % store['listVersion']
    db = json.load(open(quest_db(world), encoding='utf8'))
    damages = witness_damages(db, exp['item'])
    assert exp['newIndex'] in damages and exp['oldIndex'] not in damages, \
        'witness entries moved: %r' % sorted(set(damages))[:10]
    assert damages.count(exp['newIndex']) == exp['witnessCount'], \
        'witness entry count changed: %d -> %d' % (exp['witnessCount'], damages.count(exp['newIndex']))
    assert db.get('POSTEA_VERSIONS:10', {}).get('materiallib:idList:3') == 2
    print('idempotence verified: %d witness entries still at %d' % (exp['witnessCount'], exp['newIndex']))

if __name__ == '__main__':
    cmd, world = sys.argv[1], sys.argv[2]
    if cmd == 'discover': discover(world)
    elif cmd == 'seed': seed(world, sys.argv[3])
    elif cmd == 'assert-shift': assert_shift(world)
    elif cmd == 'assert-idempotent': assert_idempotent(world)
    else: raise SystemExit('unknown command ' + cmd)
