"""Append source motion to a COPY of R3; never edit geometry, UVs, rig or approved files."""
import bpy,json,math,sys
from pathlib import Path
from mathutils import Matrix,Vector,Quaternion
root=Path(sys.argv[sys.argv.index('--')+1]);pipeline=root/'animation_pipeline';out=pipeline/'converted/m002_a1'
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
poses=json.loads((out/'poses-new.json').read_text());actions={}
arm.animation_data_clear()
for a in list(bpy.data.actions):bpy.data.actions.remove(a)
for clip,info in poses.items():
 arm.animation_data_create();arm.animation_data.action=None
 for j,frame in enumerate(info['frames']):
  bpy.context.scene.frame_set(j+1);write_matrices(frame,info['spine'][j],.35)
  for name in order:
   p=arm.pose.bones[name];p.rotation_mode='QUATERNION'
   for prop in ('location','rotation_quaternion','scale'):p.keyframe_insert(data_path=prop,frame=j+1)
 a=arm.animation_data.action;a.name='UAL_'+clip;a.use_fake_user=True;actions[clip]=a
 track=arm.animation_data.nla_tracks.new();track.name=a.name;track.strips.new(a.name,1,a).mute=True
arm.animation_data.action=actions['heavy_source'];bpy.context.scene.frame_set(1)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'steve-a1-motion-review.blend'))
bpy.ops.object.select_all(action='DESELECT');arm.select_set(True)
for obj in mesh_objects:obj.select_set(True)
bpy.ops.export_scene.gltf(filepath=str(out/'motion-only.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='ACTIONS',export_anim_slide_to_zero=True)
# Diagnostic contact sheet uses actual derived poses; not evidence of Minecraft rendering.
scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.render.resolution_x=320;scene.render.resolution_y=360;scene.render.resolution_percentage=100
scene.display.shading.light='STUDIO';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_shadows=True
camera=bpy.data.objects.get('Camera')
if camera is None:
 bpy.ops.object.camera_add();camera=bpy.context.object
scene.camera=camera;camera.location=(3,-5,2.5);camera.rotation_euler=(Vector((0,0,1))-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.type='ORTHO';camera.data.ortho_scale=2.7
for frame in range(1,132,10):
 scene.frame_set(frame);scene.render.filepath=str(pipeline/f'reports/m002_a1/heavy_{frame-1:03d}.png');bpy.ops.render.render(write_still=True)
