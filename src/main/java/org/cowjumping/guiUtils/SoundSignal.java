package org.cowjumping.guiUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundSignal implements Callable {
  private static final Logger myLogger = LogManager.getLogger(SoundSignal.class);
  private byte[] mBuffer;

  // final static boolean block = false;
  final static public ExecutorService myThreadPool = Executors
      .newSingleThreadExecutor();

  // This is just stuff to initialize the buffers.
  final static int msec = 100;
  static int volume = 50;

  static byte[] sound800Hz;
  static byte[] sound1200Hz;
  static byte[] sound6000Hz;
  static boolean signed = true;
  static boolean bigendian = false;
  static float frequency = 44100.0F;
  static int samplesize = 8;
  static double ttpi = (2.0 * Math.PI);

  static {
    initSoundFiles();
  }

  static private void initSoundFiles() {

    new Thread() {
      public void run() {

        sound800Hz = new byte[(int) (msec * frequency / 1000)];
        sound1200Hz = new byte[(int) (msec * frequency / 1000)];
        sound6000Hz = new byte[(int) (msec * frequency / 1000)];

        for (int i = 0; i < msec * frequency / 1000; i++)

        {

          double angle800 = i / (frequency / 800) * ttpi;
          double angle1200 = i / (frequency / 1200) * ttpi;
          double angle6000 = i / (frequency / 9200) * ttpi;

          sound800Hz[i] = (byte) (Math.sin(angle800) * volume);
          sound1200Hz[i] = (byte) (Math.sin(angle1200) * volume);
          sound6000Hz[i] = (byte) (Math.sin(angle6000) * volume);
        }
      }
    }.start();
  }

  public static void positive3() {
    playSound(sound800Hz);
    playSound(sound800Hz);
    playSound(sound1200Hz);
    
  }

  public static void flat2() {
    playSound(sound800Hz);
    playSound(sound800Hz);

  }

  public static void fail2() {
    playSound(sound1200Hz);
    playSound(sound800Hz);
  }

  public static void remind() {
    playSound(sound800Hz);
  }

  public static void remindUrgent() {
    playSound(sound1200Hz);
  }

  public static void ack() {
    playSound(sound6000Hz);
  }

  protected SoundSignal(byte[] mBuffer) {
    this.mBuffer = mBuffer;
  }

  public static void playSound(byte[] mBuffer) {

    SoundSignal mSound = new SoundSignal(mBuffer);
    myThreadPool.submit(mSound);

  }

  public Object call() {

    boolean signed = true;
    boolean bigendian = false;
    // byte[] buf;

    AudioFormat format;

    // buf = new byte[1];
    int channels = 1;

    format = new AudioFormat(frequency, samplesize, channels, signed, bigendian);

    try {
      SourceDataLine sdl = AudioSystem.getSourceDataLine(format);
      if (sdl != null && mBuffer != null) {
        sdl.open(format);

        sdl.start();

        sdl.write(mBuffer, 0, mBuffer.length);
        sdl.drain();

        sdl.stop();

        sdl.close();
      }
    } catch (Exception e) {
      myLogger.error("Error while trying to play sound: ", e);
    }
    return null;
  }

  public static void notifyNewImage() {
    // TODO Auto-generated method stub
    positive3();
   
  }
}
