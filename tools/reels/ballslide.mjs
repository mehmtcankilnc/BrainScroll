// Ball Slide: the app's rules (BallSlidePuzzle/Solver) in JS, to pick a hard board for the reel.
export const DR=[-1,0,1,0], DC=[0,1,0,-1];
export function slide(n,walls,from,dir){const p=[];let r=Math.floor(from/n),c=from%n;for(;;){const nr=r+DR[dir],nc=c+DC[dir];if(nr<0||nc<0||nr>=n||nc>=n||walls[nr*n+nc])break;r=nr;c=nc;p.push(r*n+c)}return p}
// BFS over (painted mask, ball cell): returns {par, ways (number of shortest solutions, capped), path}
export function solve(n,walls,start){
  const N=n*n,all=[...Array(N).keys()].filter(i=>!walls[i]).reduce((a,i)=>a|(1n<<BigInt(i)),0n);
  const dest=new Int16Array(N*4).fill(-1),paint=new Array(N*4).fill(0n);
  for(let c=0;c<N;c++){if(walls[c])continue;for(let d=0;d<4;d++){const p=slide(n,walls,c,d);if(!p.length)continue;dest[c*4+d]=p[p.length-1];paint[c*4+d]=p.reduce((a,i)=>a|(1n<<BigInt(i)),0n)}}
  const key=(m,c)=>m.toString(36)+'.'+c;
  const startMask=1n<<BigInt(start);let frontier=[{m:startMask,c:start,way:[]}];const seen=new Map([[key(startMask,start),1]]);let depth=0;
  while(frontier.length){
    depth++;const next=new Map();let hits=[];
    for(const s of frontier){for(let d=0;d<4;d++){const to=dest[s.c*4+d];if(to<0)continue;const nm=s.m|paint[s.c*4+d];const k=key(nm,to);
      if(seen.has(k)&&seen.get(k)<depth)continue;
      const way=[...s.way,d];
      if((nm&all)===all)hits.push(way);
      if(!next.has(k))next.set(k,{m:nm,c:to,way});}}
    if(hits.length)return{par:depth,ways:hits.length,way:hits[0]};
    for(const k of next.keys())seen.set(k,depth);
    frontier=[...next.values()];if(seen.size>400000)return null;
  }
  return null;
}
export function mulberry(a){return()=>{a|=0;a=a+0x6D2B79F5|0;let t=Math.imul(a^a>>>15,1|a);t=t+Math.imul(t^t>>>7,61|t)^t;return((t^t>>>14)>>>0)/4294967296}}
