import io.github.mohammedalamkhan.hzlab.PitchDetector;
public class BassChromaTest {
 static void run(String n,double[] fs){
  PitchDetector d=new PitchDetector(44100); int len=d.getWindowSize(); float[] b=new float[len]; float[] c=new float[12];
  for(int i=0;i<len;i++){double t=i/44100.0,v=0;for(double f:fs)v+=0.22*Math.sin(2*Math.PI*f*t);b[i]=(float)v;}
  d.analyse(b,len,c,440);
  System.out.print(n+" chroma:");for(float x:c)System.out.printf(" %.2f",x);System.out.print("\n   bass:");for(float x:d.bassChroma)System.out.printf(" %.2f",x);System.out.println();
 }
 public static void main(String[] a){
  run("A maj",new double[]{110,277.18,329.63});
  run("D maj",new double[]{146.83,369.99,440});
  run("E min",new double[]{82.41,196.00,246.94});
  run("D min7",new double[]{146.83,174.61,220,261.63});
 }
}
