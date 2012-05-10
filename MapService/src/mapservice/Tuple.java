/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.io.Serializable;

/**
 *
 * @author Jason
 */
public class Tuple implements Serializable{
    Object one;
    Object two;
    
    public Tuple(Object one, Object two) {
        this.one= one;
        this.two = two;
    }

    public Object getOne() {
        return this.one;
    }

    public Object getTwo() {
        return this.two;
    }
}
