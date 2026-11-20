"""Bake motions only into a separate copy using the established R3 retarget writer."""
import bpy,json,math,sys
from pathlib import Path
from mathutils import Matrix,Vector,Quaternion
root=Path(sys.argv[sys.argv.index('--')+1]);pipeline=root/'animation_pipeline';out=pipeline/'converted/m002_a2'
bpy.ops.wm.open_mainfile(filepath=str(pipeline/'converted/m002_r3/steve-r3-refined.blend'))
arm=bpy.data.objects['Steve_R3_refined'];mesh_objects=[o for o in bpy.context.scene.objects if o.type=='MESH']
B=Matrix(((1,0,0),(0,0,-1),(0,-1,0)));Bi=B.inverted()
def pt(p):return B@Vector(p)/16+Vector((0,0,1.5))
def quat(r):return Quaternion((0,0,1),math.radians(r['z']))@Quaternion((0,1,0),math.radians(r['y']))@Quaternion((1,0,0),math.radians(r['x']))
def bquat(q):return (B@q.to_matrix()@Bi).to_quaternion()
def source_quat(a):return Quaternion((a[3],a[0],a[1],a[2]))
rest={b.name:b.matrix_local.to_3x3().to_4x4() for b in arm.data.bones}
code=(pipeline/'scripts/blender-r3-refine.py').read_text()
exec(code[code.index("order=['pelvis'"):code.index('for clip,info in poses.items():')])
poses=json.loads((out/'poses-new.json').read_text());arm.animation_data_clear()
for a in list(bpy.data.actions):bpy.data.actions.remove(a)
for clip,info in poses.items():
 arm.animation_data_create();arm.animation_data.action=None
 for j,frame in enumerate(info['frames']):
  bpy.context.scene.frame_set(j+1);write_matrices(frame,info['spine'][j],.35)
  for name in order:
   p=arm.pose.bones[name];p.rotation_mode='QUATERNION'
   for prop in ('location','rotation_quaternion','scale'):p.keyframe_insert(data_path=prop,frame=j+1)
 a=arm.animation_data.action;a.name='UAL_'+clip;a.use_fake_user=True
 track=arm.animation_data.nla_tracks.new();track.name=a.name;track.strips.new(a.name,1,a).mute=True
bpy.context.scene.frame_set(1)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'steve-a2-motion-review.blend'))
bpy.ops.object.select_all(action='DESELECT');arm.select_set(True)
for obj in mesh_objects:obj.select_set(True)
bpy.ops.export_scene.gltf(filepath=str(out/'motion-only.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='ACTIONS',export_anim_slide_to_zero=True)
