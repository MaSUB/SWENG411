/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Observer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mst5182
 */
public class NHL {
    
    protected List <HockeyFan> fans;
    protected LinkedList <TeamData> td;
    
    public NHL () throws FileNotFoundException {
        td  = new LinkedList <TeamData>();
        String  teamName;
        int     gamesPlayed;
        int     wins;
        int     losses;
        int     otLosses;
        int     goalsFor;
        int     goalsAgainst;
        
            BufferedReader br = new BufferedReader(new FileReader("NHLData.txt"));
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    String[] s = line.split("#");
                        teamName = s[0];
                        gamesPlayed= Integer.valueOf(s[1]);
                        wins= Integer.valueOf(s[2]);
                        losses= Integer.valueOf(s[3]);
                        otLosses= Integer.valueOf(s[4]);
                        goalsFor= Integer.valueOf(s[5]);
                        goalsAgainst= Integer.valueOf(s[6]);
                        TeamData t = new TeamData(teamName,gamesPlayed,wins,losses,otLosses,goalsFor,goalsAgainst);
                        td.add(t);
                }
            } catch (IOException ex) {
                Logger.getLogger(frmObserver.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(frmObserver.class.getName()).log(Level.SEVERE, null, ex);
            }
        
    }
    
    public void addObserver(HockeyFan hf){
        
        fans.add(hf);
        
    }
    
    public void removeObserver(HockeyFan hf){
        
        fans.remove(hf);
        
    }
    
    public void NotifyAll(){
        
        for(int i = 0; i <= fans.size();i++) {
            fans.get(i).notifyFan(td);
       }
        
    }
    
}
