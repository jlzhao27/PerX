/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Jason
 */
public interface MapInterface<K1,V1, K2, V2, V3> {
    
    public void map(K1 key, V1 value, Map<K2,List<V2>> output);
    
    public void reduce (K2 key, List<V2> values, Map<K2, V3> output);
}
