import io.github.mohammedalamkhan.hzlab.*;
public class ChromaTest {
 public static void main(String[] x){
  for(double f: new double[]{82.41,110,146.83,196,246.94,329.63,440}){
   PitchDetector d=new PitchDetector(44100); int n=d.getWindowSize(); float[] b=new float[n]; float[] c=new float[12];
   for(int i=0;i<n;i++){double t=i/44100.0,v=0;double[] h={1,0.5,0.3,0.2,0.12,0.08};for(int k=0;k<h.length;k++)v+=h[k]*Math.sin(2*Math.PI*f*(k+1)*t);b[i]=(float)(v*0.6);}
   double got=d.analyse(b,n,c,440);
   System.out.printf("%7.2f got %7.2f chroma: ",f,got); for(float v:c)System.out.printf("%.2f ",v); System.out.println();
  }
  // chord A major: A2 C#4 E4
  PitchDetector d=new PitchDetector(44100); int n=d.getWindowSize(); float[] b=new float[n]; float[] c=new float[12];
  for(int i=0;i<n;i++){double t=i/44100.0,v=0;for(double f:new double[]{110,277.18,329.63})v+=0.5*Math.sin(2*Math.PI*f*t);b[i]=(float)v;}
  d.analyse(b,n,c,440); System.out.print("A major: "); for(float v:c)System.out.printf("%.2f ",v); System.out.println();
 }
}
