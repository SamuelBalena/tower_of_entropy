import bpy,json,sys
from pathlib import Path
root=Path(sys.argv[sys.argv.index('--')+1]);out=root/'animation_pipeline/converted/m002_a3';out.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(root/'animation_pipeline/input/Archer/HumanM_ArcherAnimationsFREE_2.0.blend'))
report={'objects':[(o.name,o.type) for o in bpy.data.objects],'rigs':{o.name:[b.name for b in o.data.bones] for o in bpy.data.objects if o.type=='ARMATURE'},'actions':{a.name:list(a.frame_range) for a in bpy.data.actions}}
(root/'animation_pipeline/reports/m002_a3/ARCHER_SOURCE.json').write_text(json.dumps(report,indent=2));print(json.dumps(report))
# Export a private source conversion with its original bone names for the existing sampler.
bpy.ops.object.select_all(action='SELECT')
bpy.ops.export_scene.gltf(filepath=str(out/'archer-source.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='ACTIONS',export_anim_slide_to_zero=True)
