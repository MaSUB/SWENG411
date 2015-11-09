/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Observer;

import java.util.LinkedList;

/**
 *
 * @author Mason
 */
public class Weather {
    
    private LinkedList<WeatherObserver> weatherWatchers;
    private boolean isWeather;
    int pressure;
    int windSpeed;
    
    Weather(){
        
        isWeather = true;
        weatherWatchers = new LinkedList<WeatherObserver>();
        pressure = 0;
        windSpeed = 0;
        
    }
    public void subscribe(WeatherObserver o) {
        
        weatherWatchers.add(o);
        
    }
    
    public void unsubscribe(WeatherObserver o) {
        
        weatherWatchers.remove(o);
        
    }
    
     public void run(){
        while (true){
            if (this.isWeather){
                if(weatherWatchers.contains(WindSpeed.class)){
                    windSpeed = (int)Math.random();
                }
                else if(weatherWatchers.contains(Pressure.class)){
                    pressure = (int)Math.random();
                }
                notifyObservers();
            } 
            try {
                Thread.sleep(2000);}
            catch (Exception e){}
            
        }
    }

    private void notifyObservers() {
        for(int i = 0;i <= weatherWatchers.size()-1;i++){
            if(weatherWatchers.get(i).equals(WindSpeed.class)){
                weatherWatchers.get(i).update(windSpeed);
            }
            else if(weatherWatchers.get(i).equals(Pressure.class)){
                weatherWatchers.get(i).update(pressure);
            }
            
        }
        
    }
    
    public LinkedList<WeatherObserver> getList(){
        
        return weatherWatchers;
        
    }
    
}
