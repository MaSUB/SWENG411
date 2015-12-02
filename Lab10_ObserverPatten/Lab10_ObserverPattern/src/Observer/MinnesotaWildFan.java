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
public class MinnesotaWildFan implements HockeyFan {

    TeamData teamData;
    @Override
    public void notifyFan(List <TeamData> td) {
        
        for(int i = 0; i <= td.size(); i++) {
            
            if(td.get(i).teamName == "Minnesota Wild") {
                
                teamData = td.get(i);
                
            }
            
        }
        
    }
    
}
