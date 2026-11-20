import bpy,json,sys,shutil
from pathlib import Path
root=Path(sys.argv[sys.argv.index('--')+1]);source=Path('C:/Users/balen/Desktop/tower-of-entroy/assets/source/Human Archer Animations FREE/Animations/Male');out=root/'animation_pipeline/converted/m002_a3/archer';out.mkdir(parents=True,exist_ok=True)
names=['BowIdle01','BowIdle02','BowShot01 - Load','BowShot01 - Hold','BowShot01 - Release','Walk01_Backward','Walk01_Left','Walk01_Right'];report={}
for name in names:
 filename='HumanM@'+name+'.fbx';file=next(source.rglob(filename));copy=root/'animation_pipeline/input/Archer'/filename;shutil.copy2(file,copy)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.fbx(filepath=str(copy))
 arms=[o for o in bpy.data.objects if o.type=='ARMATURE'];actions=list(bpy.data.actions)
 report[name]={'bones':[[b.name for b in a.data.bones] for a in arms],'fps':bpy.context.scene.render.fps,'actions':{a.name:list(a.frame_range) for a in actions}}
 bpy.ops.export_scene.gltf(filepath=str(out/(name.replace(' ','_')+'.glb')),export_format='GLB',export_animations=True,export_animation_mode='ACTIONS',export_anim_slide_to_zero=True)
(root/'animation_pipeline/reports/m002_a3/ARCHER_FBX.json').write_text(json.dumps(report,indent=2))
