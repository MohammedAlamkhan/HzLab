import io.github.mohammedalamkhan.hzlab.PitchDetector;
public class ChordScoreTest {
 static final int[][] INT={{0,4,7},{0,4,7,11},{0,4,7,10},{0,3,7},{0,3,7,10},{0,3,6,9}};
 static final String[] N={"maj","maj7","dom7","min","min7","dim"};
 static float[] score(float[] c, float[] bass, int rootBias){
  float[] s=new float[72]; float total=0; for(float v:c)total+=v;
  for(int root=0;root<12;root++)for(int type=0;type<6;type++){
   float hit=0,w=0; boolean[] ct=new boolean[12]; int[] inv=INT[type];
   for(int i=0;i<inv.length;i++){int pc=(root+inv[i])%12;ct[pc]=true;float ww=i==0?1.55f:1f;hit+=c[pc]*ww;w+=ww;}
   float out=0;for(int pc=0;pc<12;pc++)if(!ct[pc])out+=c[pc];
   float sc=hit/w-out*0.30f;sc+=c[root]*0.18f; sc+=bass[root]*0.22f;s[root*6+type]=Math.max(0,sc);
  }
  float max=0;for(float v:s)max=Math.max(max,v);if(max>0)for(int i=0;i<s.length;i++)s[i]/=max;return s;
 }
 static void run(String name,double[] freqs){
  PitchDetector d=new PitchDetector(44100);int n=d.getWindowSize();float[] b=new float[n];float[] c=new float[12];
  for(int i=0;i<n;i++){double t=i/44100.0,v=0;for(double f:freqs)v+=0.22*Math.sin(2*Math.PI*f*t);b[i]=(float)v;}
  double got=d.analyse(b,n,c,440); float[] s=score(d.chordChroma,d.bassChroma,0);
  int bi=0;for(int i=1;i<s.length;i++)if(s[i]>s[bi])bi=i;
  System.out.printf("%s got %.2f top=pc%d %s %.3f%n  chroma:",name,got,bi/6,N[bi%6],s[bi]);
  for(int i=0;i<12;i++)System.out.printf(" %.2f",d.chordChroma[i]); System.out.println();
  // top 5
  for(int k=0;k<5;k++){int m=0;for(int i=1;i<s.length;i++)if(s[i]>s[m])m=i;System.out.printf("   pc%d %s %.3f%n",m/6,N[m%6],s[m]);s[m]=-1;}
 }
 public static void main(String[] a){
  run("A major",new double[]{110,277.18,329.63});
  run("D major",new double[]{146.83,369.99,440});
  run("E minor",new double[]{82.41,196.00,246.94});
  run("D min7",new double[]{146.83,174.61,220,261.63});
  run("G major",new double[]{98,246.94,392});
  run("C major",new double[]{130.81,261.63,329.63});
  run("E major",new double[]{82.41,164.81,207.65,329.63});
 }
}
