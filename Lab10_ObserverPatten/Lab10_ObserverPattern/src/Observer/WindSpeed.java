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
public class WindSpeed implements WeatherObserver {

    int windSpeed;
    @Override
    public void update(int w) {
        windSpeed = w;
    }

    @Override
    public int getValue() {
        return windSpeed;
    }
    
}
