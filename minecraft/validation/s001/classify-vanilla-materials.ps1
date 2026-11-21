param(
    [Parameter(Mandatory = $true)][string]$Registry,
    [string]$Index = 'docs/S001_7_2_VANILLA_BLOCKS.csv',
    [string]$Mappings = 'shaderpacks/Tower of Entropy - Fantasy Foundation/shaders/block.properties'
)
$ErrorActionPreference = 'Stop'
$rules = @(
    @{Id=11112; Family='translucent dielectric'; Names='^(?:.*_)?(?:glass|glass_pane)$|^(ice|slime_block|honey_block|tinted_glass)$' }
    @{Id=11113; Family='emissive'; Names='^(glowstone|sea_lantern|shroomlight|(?:ochre|verdant|pearlescent)_froglight|(?:soul_)?(?:wall_)?torch|(?:soul_)?lantern|end_rod|jack_o_lantern|glow_lichen|magma_block)$' }
    @{Id=11114; Family='copper patina'; Names='^(waxed_)?(weathered|oxidized)_.*copper' }
    @{Id=11107; Family='ore matrix'; Names='(?:_ore|ancient_debris|raw_(?:iron|gold|copper)_block)$' }
    @{Id=11104; Family='iron'; Names='^(iron_block|iron_bars|(?:chipped_|damaged_)?anvil|chain|iron_(?:door|trapdoor)|heavy_weighted_pressure_plate|cauldron|(?:water|lava|powder_snow)_cauldron|hopper|blast_furnace|brewing_stand|lightning_rod|rail|powered_rail|detector_rail|activator_rail)$' }
    @{Id=11105; Family='gold'; Names='^(gold_block|light_weighted_pressure_plate)$' }
    @{Id=11106; Family='copper'; Names='^(?:(?:waxed_)?(?:exposed_)?(?:cut_)?copper(?:_block|_stairs|_slab)?)$' }
    @{Id=11109; Family='dense vitreous'; Names='obsidian|^(packed_ice|blue_ice)$' }
    @{Id=11116; Family='crystal'; Names='amethyst|^(diamond_block|emerald_block|netherite_block)$' }
    @{Id=11111; Family='foliage'; Names='leaves|sapling|azalea|grass$|fern$|vine|moss_block|moss_carpet|mushroom|fungus|roots$|lichen|seagrass|kelp|coral|bamboo_sapling|(?:_)?propagule|^(dandelion|poppy|blue_orchid|allium|azure_bluet|.*_tulip|oxeye_daisy|cornflower|lily_of_the_valley|lily_pad|wither_rose|sunflower|lilac|rose_bush|peony|torchflower(?:_crop)?|pitcher_(?:plant|crop)|wheat|carrots|potatoes|beetroots|sweet_berry_bush|nether_wart|cactus|sugar_cane|dead_bush|spore_blossom|big_dripleaf(?:_stem)?|small_dripleaf|bamboo|cocoa|pumpkin(?:_stem)?|melon(?:_stem)?|attached_(?:pumpkin|melon)_stem|chorus_(?:plant|flower)|pink_petals|hanging_roots|spore_blossom)$' }
    @{Id=11103; Family='wood'; Names='^(?:stripped_)?(?:oak|spruce|birch|jungle|acacia|dark_oak|mangrove|cherry|bamboo|crimson|warped)_(?:planks|log|wood|stem|hyphae|fence|fence_gate|door|trapdoor|stairs|slab|button|pressure_plate|mosaic|mosaic_stairs|mosaic_slab|block)$|^(barrel|crafting_table|loom|bookshelf|chiseled_bookshelf|note_block|jukebox|lectern|cartography_table|fletching_table|smithing_table|ladder|composter)$' }
    @{Id=11110; Family='fiber and terracotta'; Names='(?:_wool|_carpet|(?<!_glazed)_terracotta)$|^terracotta$' }
    @{Id=11115; Family='concrete and ceramic'; Names='(?:_concrete|_glazed_terracotta)$' }
    @{Id=11102; Family='masonry'; Names='brick|tiles|prismarine|chiseled|polished|pillar|smooth_(?:stone|sandstone|red_sandstone|quartz)|purpur' }
    @{Id=11101; Family='stone'; Names='stone|deepslate|andesite|granite|diorite|basalt|calcite|dripstone|netherrack|sandstone|quartz|^(tuff|bedrock|crying_obsidian)$' }
    @{Id=11108; Family='soil'; Names='dirt|podzol|mycelium|gravel|clay|mud|snow|concrete_powder|^(grass_block|farmland|soul_sand|soul_soil|sand|red_sand)$' }
)
$stateEmissive = '^(?:redstone_lamp|redstone_torch|redstone_wall_torch|(?:soul_)?campfire|(?:.*_)?candle(?:_cake)?|copper_bulb)$'
$rows = @(Import-Csv -LiteralPath $Registry | Sort-Object block)
if (!$rows.Count -or @($rows.block | Sort-Object -Unique).Count -ne $rows.Count) { throw 'Registry is empty or contains duplicate IDs' }
$tokens = @{}
$coverage = foreach ($row in $rows) {
    if ($row.block -notmatch '^minecraft:[a-z0-9_]+$') { throw "Unexpected registry entry $($row.block)" }
    $name = $row.block.Substring(10)
    $family = 'generic dielectric'
    $id = ''
    $route = 'terrain'
    $included = $true
    if ($name -in @('water','bubble_column')) {
        $family = 'water'
        $id = '11001'
        $route = 'fluid; bubble column renders its water state'
    } elseif ($name -eq 'lava') {
        $family = 'lava'
        $id = '11002/11003'
        $route = 'fluid source/flow in opaque terrain'
    } elseif ($row.model -ne 'true') {
        if ($row.block_entity -eq 'true' -or $row.special_model -eq 'true') {
            $family = 'special renderer fallback'
            $route = 'block entity or moving block; preserves special shader dispatch'
        } else {
            $family = 'invisible technical'
            $route = 'excluded; no world model or block entity'
            $included = $false
        }
    } else {
        if ($row.translucent -eq 'true') { $route = 'translucent' }
        elseif ($row.cutout -eq 'true') { $route = 'cutout' }
        elseif ($name -eq 'tripwire') { $route = 'tripwire; renderer-specific alpha path' }
        if ($row.block_entity -eq 'true') { $route += ' + special block-entity component' }
        if ($name -match $stateEmissive -and [int]$row.emission_max -gt 0) {
            $family = 'state emission + dielectric fallback'
            $id = '11113:lit=true'
            $tokens[$row.block+':lit=true'] = 11113
        } else {
            foreach ($rule in $rules) {
                if ($name -match $rule.Names) {
                    $family = $rule.Family
                    $id = [string]$rule.Id
                    $tokens[$row.block] = $rule.Id
                    break
                }
            }
        }
    }
    $row | Add-Member -NotePropertyMembers @{renderable=$included; family=$family; shader_id=$id; route=$route} -PassThru
}
$columns = @('block','renderable','family','shader_id','route','model','solid','cutout','translucent','special_model','block_entity','fluid','waterloggable','animated_sprite','emission_max')
$coverage | Select-Object -Property $columns | Export-Csv -LiteralPath $Index -Encoding UTF8 -NoTypeInformation
$lines = @(
    'block.11001=minecraft:water minecraft:bubble_column'
    'block.11002=minecraft:lava:level=0'
    'block.11003='+((1..15 | ForEach-Object {"minecraft:lava:level=$_"}) -join ' ')
    ''
    '# Generated from the Forge 1.20.1 inventory by validation/s001/classify-vanilla-materials.ps1.'
)
foreach ($id in ($tokens.Values | Sort-Object -Unique)) {
    $members = @($tokens.Keys | Where-Object {$tokens[$_] -eq $id} | Sort-Object)
    $lines += "block.$id="+($members -join ' ')
}
[IO.File]::WriteAllLines((Join-Path (Get-Location).Path $Mappings),[string[]]$lines,[Text.UTF8Encoding]::new($false))
$coverage | Group-Object family | Sort-Object Name | Select-Object Name,Count
