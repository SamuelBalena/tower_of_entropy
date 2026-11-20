"""Bake motions only into a separate copy using the established R3 retarget writer."""
import bpy,copy,json,math,sys
from pathlib import Path
from mathutils import Matrix,Vector,Quaternion
root=Path(sys.argv[sys.argv.index('--')+1]);pipeline=root/'animation_pipeline';out=pipeline/('converted/wall-prototypes' if '--wall-prototypes' in sys.argv else 'converted/parkour' if '--parkour' in sys.argv else 'converted/expansion' if '--expansion' in sys.argv else 'converted/m002_a3')
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

def smoothstep(value):
 t=max(0,min(value,1))
 return t*t*(3-2*t)

if '--wall-prototypes' in sys.argv:
 approved=json.loads((pipeline/'converted/parkour/poses-new.json').read_text())
 audit=[]
 monitored=('head','head_outer','body','body_outer','limb_2','limb_2_outer','limb_3','limb_3_outer')

 def penetration(plane,top):
  deps=bpy.context.evaluated_depsgraph_get()
  values={}
  for obj in mesh_objects:
   if obj.name not in monitored:continue
   mesh=obj.evaluated_get(deps)
   points=(mesh.matrix_world@v.co for v in mesh.data.vertices)
   values[obj.name]=max((p.y*.9375-plane for p in points if p.z*.9375 < top),default=-1)
  return values

 def ledge_clearance(frame,raw,j,count):
  base=approved['ledge_climb']['frames'][j]
  time=j*18/(count-1)
  top=1.76-1.775*smoothstep(time/11)
  plane=.30-.68*smoothstep((time-11)/7)
  write_matrices(base,raw,.35)
  limits=penetration(plane,top)
  # Preserve the original contact envelope per mesh, allowing two millimeters of sampling tolerance.
  limits={name:max(.002,value+.002) for name,value in limits.items()}
  for weight in (1,.8,.6,.4,.2,0):
   adjusted=copy.deepcopy(base)
   for original,sampled in zip(adjusted['components'],frame['components'],strict=True):
    assert original['storeID']==sampled['storeID']
    for axis in ('x','y','z'):
     start=original['rotation'][axis]
     original['rotation'][axis]=start+weight*((sampled['rotation'][axis]-start+180)%360-180)
   write_matrices(adjusted,raw,.35)
   values=penetration(plane,top)
   if all(values[name]<=limits[name]+1e-5 for name in values):
    audit.append({'frame':j,'sourceWeightScale':weight,'penetration':values})
    return
  raise ValueError('Ledge prototype could not preserve the original clearance envelope')

for a in list(bpy.data.actions):bpy.data.actions.remove(a)
for clip,info in poses.items():
 arm.animation_data_create();arm.animation_data.action=None
 for j,frame in enumerate(info['frames']):
  bpy.context.scene.frame_set(j+1)
  if clip=='ledge_climb_ual2':ledge_clearance(frame,info['spine'][j],j,len(info['frames']))
  else:write_matrices(frame,info['spine'][j],.35)
  for name in order:
   p=arm.pose.bones[name];p.rotation_mode='QUATERNION'
   for prop in ('location','rotation_quaternion','scale'):p.keyframe_insert(data_path=prop,frame=j+1)
 a=arm.animation_data.action;a.name='UAL_'+clip;a.use_fake_user=True
 track=arm.animation_data.nla_tracks.new();track.name=a.name;track.strips.new(a.name,1,a).mute=True
if '--wall-prototypes' in sys.argv:(out/'ledge-clearance.json').write_text(json.dumps(audit,indent=2))
bpy.context.scene.frame_set(1)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'steve-a3-motion-review.blend'))
bpy.ops.object.select_all(action='DESELECT');arm.select_set(True)
for obj in mesh_objects:obj.select_set(True)
bpy.ops.export_scene.gltf(filepath=str(out/'motion-only.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='ACTIONS',export_anim_slide_to_zero=True)
