const fs=require('fs'),path=require('path');const dir=path.resolve(__dirname,'../reports/m002_a1_2');
const out={};for(const version of ['before','after']){const data=JSON.parse(fs.readFileSync(path.join(dir,version,'BENCHMARK.json')));out[version]=data.rounds.map(r=>({work:r.workload,round:r.round,maxFrameMs:Math.max(...r.frameMs),framesOver25ms:r.frameMs.filter(v=>v>25).length,maxPipelineMs:Math.max(...r.pipelineMs),worstFrameMs:[...r.frameMs].sort((a,b)=>b-a).slice(0,5)}));}
fs.writeFileSync(path.join(dir,'FRAME_STALLS.json'),JSON.stringify(out,null,2));console.log(out);
