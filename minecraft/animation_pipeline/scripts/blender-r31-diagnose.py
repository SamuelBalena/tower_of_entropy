"""Evaluate all 45 Roll samples with LBS, not a beauty render."""
import bpy,bmesh,json,sys,math
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
root=Path(sys.argv[sys.argv.index('--')+1]);variant=sys.argv[-1] if sys.argv[-1] in ('r3','r31') else 'r3'
source=root/('animation_pipeline/converted/m002_r3/steve-r3-refined.blend' if variant=='r3' else 'animation_pipeline/converted/m002_r3_1/steve-r31-refined.blend')
bpy.ops.wm.open_mainfile(filepath=str(source));arm=next(o for o in bpy.context.scene.objects if o.type=='ARMATURE');arm.animation_data.action=bpy.data.actions['UAL_roll']
objects=[o for o in bpy.context.scene.objects if o.type=='MESH' and not o.name.endswith('_outer')]
rest={};rings={}
for o in objects:
 bm=bmesh.new();bm.from_mesh(o.data);rest[o.name]=abs(bm.calc_volume(signed=True));bm.free()
 levels={}
 for v in o.data.vertices:levels.setdefault(round(v.co.z,6),[]).append(v.index)
 rings[o.name]=[ids for ids in levels.values() if len(ids)==4]
def area(v):return ((v[1]-v[0]).cross(v[2]-v[0]).length+(v[2]-v[0]).cross(v[3]-v[0]).length)/2
rest_area={o.name:[area([o.data.vertices[i].co for i in ids]) for ids in rings[o.name]] for o in objects}
rows=[]
for frame in range(1,46):
 bpy.context.scene.frame_set(frame)
 if variant=='r31':
  for o in objects:
   if o.data.shape_keys:
    samples=json.loads((root/'animation_pipeline/converted/m002_r3_1/gltf-roll-samples.json').read_text());index=min(range(len(samples)),key=lambda i:abs(samples[i]['time']-(frame-1)/30))
    for i,k in enumerate(o.data.shape_keys.key_blocks):k.value=1 if i==1+index else 0
 dg=bpy.context.evaluated_depsgraph_get();meshes={};metrics={};trees={}
 for o in objects:
  eo=o.evaluated_get(dg);m=eo.to_mesh();bm=bmesh.new();bm.from_mesh(m);volume=abs(bm.calc_volume(signed=True));bm.free()
  verts=[eo.matrix_world@v.co for v in m.vertices];faces=[tuple(p.vertices) for p in m.polygons]
  ratios=[area([verts[i] for i in ids])/a for ids,a in zip(rings[o.name],rest_area[o.name]) if a>1e-9]
  metrics[o.name]={'volumeRatio':volume/rest[o.name],'minRingAreaRatio':min(ratios),'minZ':min(v.z for v in verts)}
  trees[o.name]=BVHTree.FromPolygons(verts,faces,all_triangles=False,epsilon=0)
  meshes[o.name]=(verts,faces);eo.to_mesh_clear()
 overlaps={}
 for a,b in [('head','body'),('body','limb_4'),('body','limb_5'),('limb_4','limb_5'),('limb_2','body'),('limb_3','body'),('limb_2','limb_4'),('limb_3','limb_5')]:
  overlaps[a+'/'+b]=len(trees[a].overlap(trees[b]))
 angles={}
 for a,b in [('pelvis','4'),('pelvis','5'),('4','104'),('5','105'),('2','102'),('3','103'),('pelvis','1')]:
  qa=(arm.pose.bones[a].matrix@arm.data.bones[a].matrix_local.inverted()).to_quaternion();qb=(arm.pose.bones[b].matrix@arm.data.bones[b].matrix_local.inverted()).to_quaternion()
  q=qa.rotation_difference(qb);angle=math.degrees(q.angle);angles[a+'/'+b]=min(angle,360-angle)
 rows.append({'frame':frame,'time':(frame-1)/30,'meshes':metrics,'surfaceTrianglePairs':overlaps,'relativeAnglesDegrees':angles})
critical=sorted(rows,key=lambda r:min(m['minRingAreaRatio'] for m in r['meshes'].values()))[:10]
report={'variant':variant,'frames':rows,'criticalByRingArea':[r['frame'] for r in critical],'method':'Blender evaluated linear skinning; volumes vs rest; cross-section quad area; BVH surface pairs are a proxy, not intersection volume and may include touching seams','shapeKeys':sum(len(o.data.shape_keys.key_blocks) if o.data.shape_keys else 0 for o in objects)}
if variant=='r31':
 topology=[]
 for obj in [o for o in bpy.context.scene.objects if o.type=='MESH']:
  bm=bmesh.new();bm.from_mesh(obj.data);topology.append({'object':obj.name,'boundaryEdges':sum(e.is_boundary for e in bm.edges),'vertices':len(obj.data.vertices)});bm.free()
 (root/'animation_pipeline/reports/M002_R3_1_TOPOLOGY.json').write_text(json.dumps({'meshes':topology},indent=2))
(root/f'animation_pipeline/reports/M002_R3_1_DIAGNOSIS_{variant.upper()}.json').write_text(json.dumps(report,indent=2))
for r in critical[:5]:print('Critical',r['frame'],{n:round(m['minRingAreaRatio'],3) for n,m in r['meshes'].items()},r['relativeAnglesDegrees'])
