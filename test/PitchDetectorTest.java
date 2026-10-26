import io.github.mohammedalamkhan.hzlab.PitchDetector;
import java.util.Random;

public class PitchDetectorTest {
    static double test(double f, double[] harmonics, double noise, int sr) {
        PitchDetector d = new PitchDetector(sr);
        int n = d.getWindowSize();
        float[] buf = new float[n];
        float[] chroma = new float[12];
        Random rnd = new Random(42);
        // Use an integer number of cycles-ish, with 0.3s of signal.
        for (int i=0;i<n;i++) {
            double t=i/(double)sr;
            double v=0;
            for(int h=0;h<harmonics.length;h++) v += harmonics[h]*Math.sin(2*Math.PI*f*(h+1)*t + 0.2*h);
            v *= 0.6;
            v += noise*(rnd.nextDouble()*2-1);
            buf[i]=(float)v;
        }
        double got=d.analyse(buf,n,chroma,440.0);
        double cents=1200*Math.log(got/f)/Math.log(2);
        System.out.printf("target %9.3f  got %9.3f  cents %+7.3f  conf %.3f  rms %.4f  chromaA %.2f%n", f,got,cents,d.confidence,d.rms,chroma[9]);
        return cents;
    }
    public static void main(String[] a) {
        double[] full={1,0.5,0.3,0.2,0.12,0.08};
        double[] weak={0.1,1,0.6,0.3,0.2,0.1};
        double[] sine={1};
        double[] guitar={0.55,0.8,0.5,0.35,0.25,0.18,0.12,0.1};
        System.out.println("--- full harmonics 44.1k ---");
        for(double f: new double[]{30.87,41.2,55,82.41,110,146.83,196,246.94,329.63,440,659.25,987.77}) test(f,full,0.002,44100);
        System.out.println("--- weak fundamental 44.1k ---");
        for(double f: new double[]{82.41,110,146.83,196,329.63}) test(f,weak,0.002,44100);
        System.out.println("--- guitar-ish 44.1k ---");
        for(double f: new double[]{30.87,41.2,82.41,110,146.83,196,246.94,329.63,440}) test(f,guitar,0.01,44100);
        System.out.println("--- 22.05k ---");
        for(double f: new double[]{30.87,82.41,110,196,440,987.77}) test(f,full,0.002,22050);
        System.out.println("--- 8k ---");
        for(double f: new double[]{30.87,82.41,110,196,440,987.77}) test(f,full,0.002,8000);
    }
}
