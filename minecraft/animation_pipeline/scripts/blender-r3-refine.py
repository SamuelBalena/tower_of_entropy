"""Refine a COPY of the approved R1 Blend. No new character or replacement motion."""
import bpy,bmesh,json,math,sys
from pathlib import Path
from mathutils import Matrix,Vector,Quaternion

root=Path(sys.argv[sys.argv.index('--')+1]);old=root/'animation_pipeline/converted/m002_r1';out=root/'animation_pipeline/converted/m002_r3';out.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(old/'steve-r1-weighted.blend'))
poses=json.loads((old/'poses.json').read_text());sources=json.loads((out/'spine-source.json').read_text())
arm=bpy.data.objects['Steve_R1_weighted'];arm.name='Steve_R3_refined';mesh_objects=[o for o in bpy.context.scene.objects if o.type=='MESH']
B=Matrix(((1,0,0),(0,0,-1),(0,-1,0)));Bi=B.inverted()
def pt(p):return B@Vector(p)/16+Vector((0,0,1.5))
def quat(r):return Quaternion((0,0,1),math.radians(r['z']))@Quaternion((0,1,0),math.radians(r['y']))@Quaternion((1,0,0),math.radians(r['x']))
def bquat(q):return (B@q.to_matrix()@Bi).to_quaternion()
def source_quat(a):return Quaternion((a[3],a[0],a[1],a[2]))
arm.animation_data_clear()
for a in list(bpy.data.actions):bpy.data.actions.remove(a)
for p in arm.pose.bones:p.matrix_basis=Matrix.Identity(4)
bpy.context.view_layer.objects.active=arm;bpy.ops.object.mode_set(mode='EDIT')
pelvis=arm.data.edit_bones.new('pelvis');pelvis.head=pt((0,12,0));pelvis.tail=pt((0,6,0))
waist=arm.data.edit_bones.new('waist');waist.head=pt((0,6,0));waist.tail=pt((0,0,0));waist.parent=pelvis
arm.data.edit_bones['1'].parent=waist
for name in ('0','2','3'):arm.data.edit_bones[name].parent=arm.data.edit_bones['1']
for name in ('4','5'):arm.data.edit_bones[name].parent=pelvis
bpy.ops.object.mode_set(mode='OBJECT')
rest={b.name:b.matrix_local.to_3x3().to_4x4() for b in arm.data.bones}
stats=[]
for obj in mesh_objects:
 if obj.name in ('body','body_outer'):
  bm=bmesh.new();bm.from_mesh(obj.data)
  vertical=[e for e in bm.edges if abs(e.verts[0].co.z-e.verts[1].co.z)>.5]
  bmesh.ops.subdivide_edges(bm,edges=vertical,cuts=5,use_grid_fill=True)
  bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(obj.data);bm.free();obj.data.update()
  # Preserve the existing skin islands: bmesh interpolates loop UVs across the original side quads.
  obj.vertex_groups.clear();groups={n:obj.vertex_groups.new(name=n) for n in ('pelvis','waist','1')}
  for v in obj.data.vertices:
   y=(1.5-v.co.z)*16
   if y<=6:
    f=max(0,min(1,y/6));weights={'1':1-f,'waist':f}
   else:
    f=max(0,min(1,(y-6)/6));weights={'waist':1-f,'pelvis':f}
   for n,w in weights.items():
    if w>0:groups[n].add([v.index],w,'REPLACE')
 elif obj.name in ('limb_4','limb_4_outer','limb_5','limb_5_outer'):
  # The hip boundary follows the same pelvis matrix as the lower shirt, closing the old waist seam.
  pg=obj.vertex_groups.get('pelvis') or obj.vertex_groups.new(name='pelvis')
  upper=obj.vertex_groups['4' if '4' in obj.name else '5']
  for v in obj.data.vertices:
   local_y=(.75-v.co.z)*16
   f=max(0,min(1,local_y/2))
   if local_y<2:
    for g in obj.vertex_groups:
     if g.name!='pelvis':g.remove([v.index])
    if f>0:upper.add([v.index],f,'REPLACE')
    pg.add([v.index],1-f,'REPLACE')
 # Skinning remains LBS as used by Polymesh. No dual-quaternion-only Blender preview trick.
 for mod in obj.modifiers:
  if mod.type=='ARMATURE':mod.use_deform_preserve_volume=False
 bm=bmesh.new();bm.from_mesh(obj.data);boundaries=sum(e.is_boundary for e in bm.edges);bm.free()
 stats.append({'object':obj.name,'vertices':len(obj.data.vertices),'faces':len(obj.data.polygons),'boundaryEdges':boundaries})

alphas={'idle':.15,'jog':.35,'sword':.35,'roll':.55};actions={};motion_stats={}
order=['pelvis','waist','1','0','2','3','4','5','102','103','104','105']
def write_matrices(frame,raw,alpha):
 comps={c['storeID']:c for c in frame['components']};chest=quat(comps[1]['rotation'])
 qpel=chest.slerp(source_quat(raw['quaternions']['pelvis']),alpha)
 qwaist=chest.slerp(source_quat(raw['quaternions']['spine_02']),alpha)
 lift=comps[4]['pos']['y']-12
 hip=Vector((0,12+lift,0));middle=hip+qpel@Vector((0,-6,0));neck=middle+qwaist@Vector((0,-6,0))
 world={'pelvis':(hip,qpel),'waist':(middle,qwaist),'1':(neck,chest)}
 for i in (0,2,3):
  pivot=(0,0,0) if i==0 else ((5,2,0) if i==2 else (-5,2,0))
  world[str(i)]=(neck+chest@Vector(pivot),quat(comps[i]['rotation']))
 for i in (4,5):world[str(i)]=(hip+qpel@Vector((1.9 if i==4 else -1.9,0,0)),quat(comps[i]['rotation']))
 for i in (102,103,104,105):
  p,q=world[str(i-100)];local=comps[i];world[str(i)]=(p+q@Vector(tuple(local['pos'].values())),q@quat(local['rotation']))
 for name in order:
  p,q=world[name];arm.pose.bones[name].matrix=Matrix.Translation(pt(p))@bquat(q).to_matrix().to_4x4()@rest[name];bpy.context.view_layer.update()
 return math.degrees(qpel.rotation_difference(chest).angle),world
for clip,info in poses.items():
 arm.animation_data_create();arm.animation_data.action=None;loop=clip in ('idle','jog');frames=info['frames'];maximum=0
 for j,frame in enumerate(frames+([frames[0]] if loop else [])):
  bpy.context.scene.frame_set(j+1);raw=sources[clip]['frames'][j%len(frames)]
  angle,world=write_matrices(frame,raw,alphas[clip]);maximum=max(maximum,min(angle,360-angle))
  for name in order:
   p=arm.pose.bones[name];p.rotation_mode='QUATERNION'
   for prop in ('location','rotation_quaternion','scale'):p.keyframe_insert(data_path=prop,frame=j+1)
 a=arm.animation_data.action;a.name='UAL_'+clip;a.use_fake_user=True;actions[clip]=a
 track=arm.animation_data.nla_tracks.new();track.name=a.name;track.strips.new(a.name,1,a).mute=True
 motion_stats[clip]={'sourceFrames':len(frames),'relativeSourceRetention':alphas[clip],'maxPelvisChestDegrees':maximum,'oldBoneGlobalOrientations':'preserved at all original sample frames','durationMs':info['duration']}
arm.animation_data.action=actions['sword'];bpy.context.scene.frame_set(7)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'steve-r3-refined.blend'))
bpy.ops.object.select_all(action='DESELECT');arm.select_set(True)
for obj in mesh_objects:obj.select_set(True)
bpy.ops.export_scene.gltf(filepath=str(out/'steve-r3-refined.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='ACTIONS',export_anim_slide_to_zero=True)
(root/'animation_pipeline/reports/M002_R3_REFINEMENT.json').write_text(json.dumps({'sourceBlend':'m002_r1/steve-r1-weighted.blend','method':'load existing model; subdivide torso sides; add pelvis/waist; reparent existing chest/head/limbs; resample original UAL spine channels','bones':len(arm.data.bones),'newBones':['pelvis','waist'],'meshes':stats,'motions':motion_stats,'headOrientation':'original global quaternion preserved','hipSeam':'top leg rings and lower torso share pelvis influence','uv':'existing 64x64 loop UVs interpolated; no replacement texture'},indent=2))
print('R3 refinement:',json.dumps(motion_stats))
