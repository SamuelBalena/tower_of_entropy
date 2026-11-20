"""Original R1 weighted-mesh reference, not an Epic Fight asset or a Minecraft renderer.
Run with installed Blender --background --python this.py -- <minecraft root>.
"""
import bpy, bmesh, json, math, sys
from pathlib import Path
from mathutils import Matrix, Vector, Quaternion

root=Path(sys.argv[sys.argv.index('--')+1]); out=root/'animation_pipeline/converted/m002_r1'
if '--export-only' in sys.argv:
 bpy.ops.wm.open_mainfile(filepath=str(out/'steve-r1-weighted.blend'))
 bpy.ops.object.select_all(action='DESELECT')
 for obj in bpy.context.scene.objects:
  if obj.type in ('ARMATURE','MESH'):obj.select_set(True)
 bpy.ops.export_scene.gltf(filepath=str(out/'steve-r1-weighted.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='ACTIONS',export_anim_slide_to_zero=True)
 sys.exit(0)
poses=json.loads((out/'poses.json').read_text()); bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
B=Matrix(((1,0,0),(0,0,-1),(0,-1,0))); Bi=B.inverted()
def pt(p):return B@Vector(p)/16+Vector((0,0,1.5))
def quat(c):
 r=c['rotation'];return (Quaternion((0,0,1),math.radians(r['z']))@Quaternion((0,1,0),math.radians(r['y']))@Quaternion((1,0,0),math.radians(r['x'])))
def bquat(q):return (B@q.to_matrix()@Bi).to_quaternion()
pivots={0:(0,0,0),1:(0,0,0),2:(5,2,0),3:(-5,2,0),4:(1.9,12,0),5:(-1.9,12,0)}
for i in (2,3,4,5):pivots[100+i]=tuple(Vector(pivots[i])+Vector((0,4 if i<4 else 6,0)))
armdata=bpy.data.armatures.new('Steve_R1_weighted'); arm=bpy.data.objects.new('Steve_R1_weighted',armdata);bpy.context.collection.objects.link(arm);bpy.context.view_layer.objects.active=arm
bpy.ops.object.mode_set(mode='EDIT')
for i,p in pivots.items():
 b=armdata.edit_bones.new(str(i));b.head=pt(p);b.tail=pt(Vector(p)+Vector((0,6,0)))
 if i>=100:b.parent=armdata.edit_bones[str(i-100)]
bpy.ops.object.mode_set(mode='OBJECT');arm.show_in_front=True
mat=bpy.data.materials.new('Vanilla_Steve_64x64_nearest');mat.use_nodes=True
img=bpy.data.images.load(str(out/'steve.png'));img.pack();tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=img;tex.interpolation='Closest'
bs=mat.node_tree.nodes.get('Principled BSDF');mat.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color']);mat.node_tree.links.new(tex.outputs['Alpha'],bs.inputs['Alpha']);bs.inputs['Roughness'].default_value=.8
mat.surface_render_method='DITHERED'
meshes=[];statistics=[]
def box(name,p,offset,size,u,v,bone,lower=None,inflate=0):
 x0,y0,z0=offset;w,h,d=size
 # Shared cross-section vertices: connected skin surface, not individually capped halves.
 rings=sorted(set([0,h]+([2,4,5,6,7,8,10] if lower else [])))
 rings=[s for s in rings if 0<=s<=h]
 verts=[];weights=[]
 for y in rings:
  for x,z in [(0,0),(w,0),(w,d),(0,d)]:
   local=(x0+x+(-inflate if x==0 else inflate),y0+y+(-inflate if y==0 else inflate if y==h else 0),z0+z+(-inflate if z==0 else inflate))
   verts.append(pt(Vector(p)+Vector(local)))
   bend=4 if bone in (2,3) else 6
   f=max(0,min(1,(y0+y-(bend-1))/2)) if lower else 0
   weights.append(f)
 faces=[];uvs=[]
 # North, east, south, west side islands of the standard Steve skin.
 starts=[u+d,u+d+w,u+2*d+w,u]
 widths=[w,d,w,d]
 for j in range(len(rings)-1):
  for k in range(4):
   faces.append((j*4+k,j*4+(k+1)%4,(j+1)*4+(k+1)%4,(j+1)*4+k))
   a=starts[k];b=a+widths[k];top=v+d+rings[j];bottom=v+d+rings[j+1]
   uvs.append([(a,top),(b,top),(b,bottom),(a,bottom)])
 faces.extend([(3,2,1,0),tuple((len(rings)-1)*4+k for k in range(4))])
 uvs.extend([[(u+d,v),(u+d+w,v),(u+d+w,v+d),(u+d,v+d)],[(u+d+w,v+d),(u+d+2*w,v+d),(u+d+2*w,v),(u+d+w,v)]])
 mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],faces);mesh.update();obj=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(obj);obj.data.materials.append(mat)
 uv=mesh.uv_layers.new(name='Steve64')
 for poly,coords in zip(mesh.polygons,uvs):
  for loop,c in zip(poly.loop_indices,coords):uv.data[loop].uv=(c[0]/64,1-c[1]/64)
 bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(mesh);bm.free()
 upper=obj.vertex_groups.new(name=str(bone));low=obj.vertex_groups.new(name=str(lower)) if lower else None
 for i,f in enumerate(weights):
  if 1-f>0:upper.add([i],1-f,'REPLACE')
  if low and f>0:low.add([i],f,'REPLACE')
 mod=obj.modifiers.new('Continuous_LBS','ARMATURE');mod.object=arm;mod.use_deform_preserve_volume=False;obj.parent=arm;meshes.append(obj)
 statistics.append({'name':name,'vertices':len(verts),'faces':len(faces),'rings':rings,'connected_joint':bool(lower),'weights_normalized':True,'uv':'Steve64 unchanged side islands'})
for name,offset,size,u,v,bone in [('head',(-4,-8,-4),(8,8,8),0,0,0),('body',(-4,0,-2),(8,12,4),16,16,1)]:
 box(name,(0,0,0),offset,size,u,v,bone)
 box(name+'_outer',(0,0,0),offset,size,32 if bone==0 else 16,0 if bone==0 else 32,bone,inflate=.5 if bone==0 else .25)
for i,u,v,ou,ov,x,y in [(2,32,48,48,48,-1,-2),(3,40,16,40,32,-3,-2),(4,16,48,0,48,-2,0),(5,0,16,0,32,-2,0)]:
 box('limb_'+str(i),pivots[i],(x,y,-2),(4,12,4),u,v,i,100+i)
 box('limb_'+str(i)+'_outer',pivots[i],(x,y,-2),(4,12,4),ou,ov,i,100+i,.25)
rest={i:armdata.bones[str(i)].matrix_local.to_3x3().to_4x4() for i in pivots}
def apply(frame):
 comps={c['storeID']:c for c in frame['components']}; world={}
 for i in pivots:
  c=comps[i];q=quat(c);p=Vector(tuple(c['pos'].values()))
  if i>=100:
   pp,pq=world[i-100];p=pp+pq@p;q=pq@q
  world[i]=(p,q)
  arm.pose.bones[str(i)].matrix=Matrix.Translation(pt(p))@bquat(q).to_matrix().to_4x4()@rest[i]
  bpy.context.view_layer.update()
 return world
actions={}
for clip,info in poses.items():
 arm.animation_data_create();arm.animation_data.action=None
 frames=info['frames'];loop=clip in ('idle','jog')
 for j,frame in enumerate(frames+([frames[0]] if loop else [])):
  number=j+1;bpy.context.scene.frame_set(number);apply(frame)
  for b in arm.pose.bones:
   b.rotation_mode='QUATERNION'
   for prop in ('location','rotation_quaternion','scale'):b.keyframe_insert(data_path=prop,frame=number)
 action=arm.animation_data.action;action.name='UAL_'+clip;action.use_fake_user=True;actions[clip]=action
 track=arm.animation_data.nla_tracks.new();track.name=action.name;strip=track.strips.new(action.name,1,action);strip.mute=True
arm.animation_data.action=actions['sword'];scene=bpy.context.scene;scene.render.fps=30;scene.frame_start=1;scene.frame_end=76
# Camera and neutral lighting are for this offline reference only.
def aim(obj,target):obj.rotation_euler=(Vector(target)-obj.location).to_track_quat('-Z','Y').to_euler()
bpy.ops.object.camera_add(location=(3.2,5.2,2.8));camera=bpy.context.object;aim(camera,(0,0,1));camera.data.type='ORTHO';camera.data.ortho_scale=2.8;scene.camera=camera
bpy.ops.object.light_add(type='AREA',location=(2,4,5));bpy.context.object.data.energy=450;bpy.context.object.data.shape='DISK';bpy.context.object.data.size=5;aim(bpy.context.object,(0,0,1))
scene.world.color=(.3,.3,.3);scene.render.engine='CYCLES';scene.cycles.samples=24;scene.render.resolution_x=850;scene.render.resolution_y=850;scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG';scene.view_settings.view_transform='Standard'
for clip,info in poses.items():
 arm.animation_data.action=actions[clip];scene.frame_set(info['index']+1);bpy.context.view_layer.update()
 scene.render.filepath=str(out/('weighted-'+clip+'.png'));bpy.ops.render.render(write_still=True)
arm.animation_data.action=actions['sword'];scene.frame_set(7)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'steve-r1-weighted.blend'))
bpy.ops.object.select_all(action='DESELECT');arm.select_set(True)
for obj in meshes:obj.select_set(True)
# GLB reference is local and is not consumed by CPM/Full Motion automatically.
bpy.ops.export_scene.gltf(filepath=str(out/'steve-r1-weighted.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='ACTIONS',export_anim_slide_to_zero=True)
(root/'animation_pipeline/reports/M002_R1_WEIGHTED_MESH.json').write_text(json.dumps({'generator':'Original R1 script; no Epic Fight geometry or code','objects':statistics,'bones':len(pivots),'actions':list(actions),'pixel_skin':list(img.size),'runtime_import':'Not supported directly by CPM or Full Motion 1.0.0; offline weighted reference only'},indent=2))
