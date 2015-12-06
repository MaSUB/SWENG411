package Client_Side;

import java.util.LinkedList;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;


	


public final class VolumeControl
{

    private VolumeControl(){}

    private static LinkedList<Line> speakers = new LinkedList<Line>();

    private final static void findSpeakers()
    {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers)
        {
            if(!mixerInfo.getName().equals("Java Sound Audio Engine")) continue;

            Mixer mixer         = AudioSystem.getMixer(mixerInfo);
            Line.Info[] lines   = mixer.getSourceLineInfo();

            for (Line.Info info : lines)
            {

                try 
                {
                    Line line = mixer.getLine(info);
                    speakers.add(line);

                }
                catch (LineUnavailableException e)      { e.printStackTrace();                                                                                  } 
                catch (IllegalArgumentException iaEx)   {                                                                                                       }
            }
        }
    }

    static
    {
        findSpeakers();
    }

    public static void setVolume(float level)
    {
        System.out.println("setting volume to "+level);
        for(Line line : speakers)
        {
            try
            {
                line.open();
                FloatControl control = (FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
                control.setValue(limit(control,level));
            }
            catch (LineUnavailableException e) { continue; }
            catch(java.lang.IllegalArgumentException e) { continue; }



        }
    }

    private static float limit(FloatControl control,float level)
    { return Math.min(control.getMaximum(), Math.max(control.getMinimum(), level)); }

}

