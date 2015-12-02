/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Observer;

/**
 *
 * @author Mason
 */
public class Pressure implements WeatherObserver{

    int pressure;
    public void update(int w) {
        pressure = w;
    }

    @Override
    public int getValue() {
        return pressure;
    }
    
    
       
}
