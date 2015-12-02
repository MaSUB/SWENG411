/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Observer;

import java.util.List;

/**
 *
 * @author mst5182
 */
public class GeneralHockeyFan implements HockeyFan {

    List<TeamData> teamData;
    @Override
    public void notifyFan(List<TeamData> td) {
        
        teamData = td;
        
    }
    
}
