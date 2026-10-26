import io.github.mohammedalamkhan.hzlab.PitchDetector;
import java.util.Random;
public class HardPitchTest {
 static void run(String label,double f,double[] h,double noise,int sr){
  PitchDetector d=new PitchDetector(sr); int n=d.getWindowSize(); float[] b=new float[n]; float[] c=new float[12]; Random r=new Random(3);
  for(int i=0;i<n;i++){double t=i/(double)sr,v=0;for(int k=0;k<h.length;k++)v+=h[k]*Math.sin(2*Math.PI*f*(k+1)*t+0.37*k);v*=0.45;v+=noise*(r.nextDouble()*2-1);b[i]=(float)v;}
  double got=d.analyse(b,n,c,440); double cents=got>0?1200*Math.log(got/f)/Math.log(2):0;
  System.out.printf("%-18s f=%8.2f got=%9.2f cents=%+8.2f conf=%.3f rms=%.4f%n",label,f,got,cents,d.confidence,d.rms);
 }
 public static void main(String[] a){
  double[] missing={0,1,0.8,0.4,0.3,0.2}; double[] strong3={0.1,0.2,1,0.2,0.1}; double[] even={0,1,0,0.5,0,0.2};
  for(double f:new double[]{30.87,41.2,82.41,110,146.83,196,329.63,440}) run("missing-fund",f,missing,0.005,44100);
  for(double f:new double[]{82.41,110,196,440}) run("strong-3rd",f,strong3,0.005,44100);
  for(double f:new double[]{82.41,110,196,440}) run("even-only",f,even,0.005,44100);
  // plucked string: Karplus-Strong
  for(double f:new double[]{82.41,110,146.83,196,246.94,329.63}){
   int sr=44100; int n=8192; float[] b=new float[n]; Random r=new Random(5); int N=(int)Math.round(sr/f); float[] buf=new float[N];
   for(int i=0;i<N;i++)buf[i]=(float)(r.nextDouble()*2-1);
   int p=0; for(int i=0;i<n;i++){float cur=buf[p]; float nxt=buf[(p+1)%N]; float val=0.5f*(cur+nxt)*0.998f; buf[p]=val; p=(p+1)%N; b[i]=cur*0.6f;}
   PitchDetector d=new PitchDetector(sr); float[] c=new float[12]; double got=d.analyse(b,n,c,440); double cents=1200*Math.log(got/f)/Math.log(2);
   System.out.printf("Karplus %8.2f got %9.2f cents %+7.2f conf %.3f rms %.4f%n",f,got,cents,d.confidence,d.rms);
  }
 }
}
