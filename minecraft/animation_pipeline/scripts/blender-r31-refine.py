"""Bake an independent Roll volume-corrective layer. Original R3 and motions stay intact.
Regularized weights and polar rotation preserve each cross-section before inverse LBS.
The actual game still uses Polymesh CPU LBS plus glTF morph targets.
"""
import bpy,bmesh,json,sys,math,numpy as np
from pathlib import Path
from mathutils import Matrix,Vector
root=Path(sys.argv[sys.argv.index('--')+1]);out=root/'animation_pipeline/converted/m002_r3_1';out.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(root/'animation_pipeline/converted/m002_r3/steve-r3-refined.blend'))
arm=next(o for o in bpy.context.scene.objects if o.type=='ARMATURE');objects=[o for o in bpy.context.scene.objects if o.type=='MESH'];arm.animation_data.action=bpy.data.actions['UAL_roll']
changes=[]
# Existing limb rings already bracket elbows/knees. One additional hip ring gives
# the two-pixel pelvis/thigh transition a genuine midpoint, with interpolated UVs/weights.
for obj in objects:
 if obj.name.startswith(('limb_4','limb_5')):
  bm=bmesh.new();bm.from_mesh(obj.data)
  edges=[e for e in bm.edges if abs(e.verts[0].co.z-e.verts[1].co.z)>.08 and min(e.verts[0].co.z,e.verts[1].co.z)>.61]
  bmesh.ops.subdivide_edges(bm,edges=edges,cuts=1,use_grid_fill=True);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(obj.data);bm.free();obj.data.update()
  changes.append({'mesh':obj.name,'hipEdgesSubdivided':len(edges),'vertices':len(obj.data.vertices)})
regularized=0
for obj in objects:
 for v in obj.data.vertices:
  influences=[g for g in v.groups if g.weight>0]
  if len(influences)==2 and all(abs(g.weight-.5)<1e-5 for g in influences):
   # Prefer the proximal joint, avoiding singular 50/50 matrices near a 180-degree fold.
   priority={'pelvis':0,'waist':1,'1':2,'2':3,'3':3,'4':3,'5':3,'102':4,'103':4,'104':4,'105':4}
   ordered=sorted(influences,key=lambda g:priority[obj.vertex_groups[g.group].name])
   for g,w in zip(ordered,(.6,.4)):obj.vertex_groups[g.group].add([v.index],w,'REPLACE')
   regularized+=1
base={o.name:[v.co.copy() for v in o.data.vertices] for o in objects};targets={o.name:[] for o in objects};validation=[]
centers={}
for obj in objects:
 levels={}
 for v in obj.data.vertices:levels.setdefault(round(v.co.z,6),[]).append(v.index)
 centers[obj.name]={i:sum((base[obj.name][k] for k in ids),Vector())/len(ids) for ids in levels.values() for i in ids}
samples=json.loads((out/'gltf-roll-samples.json').read_text());bind=json.loads((out/'gltf-roll-bind.json').read_text())
T=Matrix(((1,0,0,0),(0,0,-1,0),(0,1,0,0),(0,0,0,1)))
def matrix(a):return Matrix(tuple(tuple(a[c*4+r] for c in range(4)) for r in range(4)))
parent_order=['pelvis','waist','1','0','2','3','4','5','102','103','104','105']
for sample,info in enumerate(samples):
 frame=1+info['time']*30;bpy.context.scene.frame_set(int(frame),subframe=frame-int(frame))
 # Match the actual exported glTF SLERP/LINEAR transforms, including rest-axis mapping.
 for name in parent_order:
  arm.pose.bones[name].matrix=T@matrix(info['bones'][name])@matrix(bind[name]).inverted()@T.inverted()@arm.data.bones[name].matrix_local;bpy.context.view_layer.update()
 matrices={b.name:arm.pose.bones[b.name].matrix@b.matrix_local.inverted() for b in arm.data.bones}
 for obj in objects:
  corrected=[];maxerror=0;maxoffset=0
  for v,p in zip(obj.data.vertices,base[obj.name]):
   m=Matrix(((0,0,0,0),)*4)
   for g in v.groups:
    name=obj.vertex_groups[g.group].name
    if name in matrices:
     mat=matrices[name]
     for row in range(4):
      for col in range(4):m[row][col]+=g.weight*mat[row][col]
   assert abs(m.determinant())>.03,('near singular',frame,obj.name,v.index)
   u,s,vt=np.linalg.svd(np.array(m.to_3x3()));rotation=Matrix((u@vt).tolist())
   center=centers[obj.name][v.index];target=m@center+rotation@(p-center)
   point=m.inverted()@target;corrected.append(point);maxerror=max(maxerror,(m@point-target).length);maxoffset=max(maxoffset,(point-p).length)
  targets[obj.name].append(corrected);validation.append({'frame':frame,'mesh':obj.name,'maxReconstructionErrorBlocks':maxerror,'maxPreSkinOffsetBlocks':maxoffset})
# Export shape keys at zero. Weight animation is patched deterministically into the
# GLB afterward, avoiding ambiguous Blender action/NLA merging of mesh and rig tracks.
for obj in objects:
 obj.shape_key_add(name='Basis')
 for frame,positions in enumerate(targets[obj.name],1):
  key=obj.shape_key_add(name=f'RollVolume_{frame:03d}')
  for v,p in zip(key.data,positions):v.co=p
  key.value=0
 for mod in obj.modifiers:
  if mod.type=='ARMATURE':mod.use_deform_preserve_volume=False
arm.animation_data.action=bpy.data.actions['UAL_roll'];bpy.context.scene.frame_set(15)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'steve-r31-refined.blend'))
bpy.ops.object.select_all(action='DESELECT');arm.select_set(True)
for obj in objects:obj.select_set(True)
bpy.ops.export_scene.gltf(filepath=str(out/'steve-r31-raw.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='ACTIONS',export_anim_slide_to_zero=True,export_morph=True,export_morph_normal=True)
(root/'animation_pipeline/reports/M002_R3_1_CORRECTIVE_BAKE.json').write_text(json.dumps({'method':'polar rotation preserves ring cross-section; inverse-LBS corrective targets sampled from actual GLB transforms at 120 Hz','source':'m002_r3/steve-r3-refined.blend','frames':45,'targetsPerMesh':len(samples),'regularizedVertices':regularized,'geometry':changes,'checks':validation,'maxReconstructionErrorBlocks':max(r['maxReconstructionErrorBlocks'] for r in validation),'originalBoneActions':'unchanged','UV':'original islands; interpolated only at new hip rings'},indent=2))
print('R31 baked; maximum reconstruction error',max(r['maxReconstructionErrorBlocks'] for r in validation))
topology=[]
for obj in objects:
 bm=bmesh.new();bm.from_mesh(obj.data);topology.append({'object':obj.name,'boundaryEdges':sum(e.is_boundary for e in bm.edges),'vertices':len(obj.data.vertices)});bm.free()
(root/'animation_pipeline/reports/M002_R3_1_TOPOLOGY.json').write_text(json.dumps({'meshes':topology},indent=2))
