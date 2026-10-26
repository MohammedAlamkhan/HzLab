import io.github.mohammedalamkhan.hzlab.PitchDetector;
public class BassSingle {
 static void run(double f){
  PitchDetector d=new PitchDetector(44100); int n=d.getWindowSize(); float[] b=new float[n]; float[] c=new float[12];
  for(int i=0;i<n;i++) b[i]=(float)(0.3*Math.sin(2*Math.PI*f*i/44100.0));
  d.analyse(b,n,c,440);
  System.out.printf("%.2f chroma:",f);for(float x:c)System.out.printf(" %.2f",x);System.out.print("  bass:");for(float x:d.bassChroma)System.out.printf(" %.2f",x);System.out.println();
 }
 public static void main(String[] a){for(double f:new double[]{82.41,110,146.83,196,246.94})run(f);}
}
