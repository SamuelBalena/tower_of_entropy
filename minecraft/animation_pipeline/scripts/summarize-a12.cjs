const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const dir=path.resolve(__dirname,'../reports/m002_a1_2');
const read=p=>JSON.parse(fs.readFileSync(path.join(dir,p),'utf8').replace(/^\uFEFF/,''));
const suites={};
for(const p of ['before/RUNTIME.json','after/RUNTIME.json','after/RESTART.json','regression_a1/RUNTIME.json','ROLL_REGRESSION.json','IFRAMES.json','COMPARISON.json','COMPARATOR_TEST.json']){
 const data=read(p),checks=data.checks;assert(Array.isArray(checks)&&checks.length,p);assert(checks.every(c=>c.pass),p);suites[p]={passed:checks.filter(c=>c.pass).length,total:checks.length};
}
const preservation=read('PRESERVATION.json'),equivalence=read('EQUIVALENCE.json');assert.equal(preservation.status,'PASS');assert(equivalence.pass);
const builds={};for(const mode of ['normal','r31']){const buffer=fs.readFileSync(path.resolve(dir,'../../../.logs/m002-a12-build-'+mode+'.log'));const log=buffer.toString(buffer[0]===255?'utf16le':'utf8');assert(log.includes('BUILD SUCCESSFUL'),mode);builds[mode]={status:'PASS',duration:log.match(/BUILD SUCCESSFUL in ([^\r\n]+)/)?.[1]};}
const artifact=read('ARTIFACT.json');assert.equal(artifact.status,'PASS');
const out={status:'TECHNICAL_PASS_VISUAL_REVIEW_REQUIRED',suites,builds,artifact,preservation,equivalence,evidence:read('EVIDENCE.json'),performance:read('PERFORMANCE.json'),performanceFirstFinalRun:read('PERFORMANCE_FINAL_RUN1.json'),aestheticApproval:'PENDING_USER'};
fs.writeFileSync(path.join(dir,'RESULTS.json'),JSON.stringify(out,null,2));console.log(JSON.stringify(suites,null,2));
