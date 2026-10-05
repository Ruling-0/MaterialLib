# MaterialLib

A library for easily adding and managing material-based items.
Designed to be highly flexible and crossmod-friendly.

## Primitives
MaterialLib's design is built on several primitives:
- **Material:** Some Property-carrying group, like Iron or Gold.
- **Shape:** The actual items, blocks, or fluids that are made
of Materials.
- **Family:** A collection of Materials, useful for sharing
Shape generation or Properties among Materials.
- **Property:** Some feature of a Material, used however the mod
needs, such as processing voltage for recipes.
- **ShapeConsumer:** A method that iterates over every generated
Shape, providing the Shape and Material. Useful for recipe or 
tool generation.

## API
Mods enter the API through `MaterialLibAPI` or `MaterialLibClient`.
`MaterialLibAPI` provides builders and registration methods for
the primitives, while `MaterialLibClient` handles texture and
renderer registration. For most simple Shapes, the builders
should suffice; however, more complex Shapes can extend
`ShapeItem`/`ShapeBlock`/`ShapeFluid`/`ShapeFluidInContainer`
to add custom behavior and register them through the API.

The mod's items and blocks are registered as one ID per Shape,
with Materials being a global metadata entry. [Postea](https://github.com/GTNewHorizons/Postea)
is used to automatically handle shifts in the metadata mapping
between mod versions or mod lists.

Where two mods register conflicting Materials/Shapes (by name),
MaterialLib handles the conflict by first choosing the canonical
one, which is by alphabetical mod ID, descending. It then unifies
other declarations onto the canonical: any non-conflicting properties
are merged in, while the canonical wins any conflicts. Mods can
register edits through the API (`MaterialEdit`/`ShapeEdit`/`FamilyEdit`),
which run after the conflict resolution.

> [!TIP]
> 
> Consider having your Shape names match their primary oredict. 
> MaterialLib then handles the old issues of many mods adding
> items like `ingotCopper`.

> [!CAUTION]
> 
> One should never rely on their Materials or Shapes taking
> priority over other mods'. It is far better to handle
> crossmod behavior explicitly, through the edits or changing
> behavior after checking if the other mod is loaded. Consider
> using Property names that are unlikely to conflict; for example,
> adding your modid like `MYMOD:PROPERTYNAME`.

## Textures
There are two supported texture systems:
- **TextureSet:** Declares a set (folder) of textures for Shapes.
Materials all carry a tint Property, which tints all their Shapes.
Textures can have multiple layers, and a final untinted overlay
texture.
- **PaletteSprites:** Each Material, in its builder, can be
given a name of a palette and an index for its column in it. Then,
rather than tint, the Shape textures will be colored using the
palette. Each pixel in the Shape texture is replaced with the
palette color by descending value (so the brightest pixel gets
the first pixel in the palette column). The last pixel is 
repeated as necessary to replace all original pixels.

> [!TIP]
> 
> The Palette system allows for more expression than the tint
> system; however, it requires more work to implement. One must
> take care that indices are properly maintained, and that the
> base textures use a number of pixels (by value) that all
> palette columns support. If some materials need to express
> more colors than others, consider repeating colors in some 
> columns.

## Examples
`ExampleContent` contains examples on using various API features.
The content can be viewed in-game by setting `registerExamples`
to `true` in MaterialLib's config.
