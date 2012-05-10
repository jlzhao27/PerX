/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

/**
 *
 * @author Jason
 */
public enum WorkState {
    WAITING_INFO("WIN"),
    WAITING_DATA ("WDA"),
    MAPPING ("MAP"),
    REDUCING("RDC"),
    CLOSING ("CLO");
    
    
    private String rep;
    
    WorkState(String s) {
        this.rep = s;
    }
    
    public static WorkState fromString(String s) {
         if (s.equals("WIN")) {
             return WorkState.WAITING_INFO;
        } else if (s.equals("WDA")) {
            return WorkState.WAITING_DATA;
        } else if (s.equals("MAP")) {
            return WorkState.MAPPING;
        } else if (s.equals("RDC")) {
            return WorkState.REDUCING;
        } else {
            return WorkState.CLOSING;
        }
        
    }
    
    @Override
    public String toString(){
        return this.rep;
    }
    
    public static final String MSG_INFO = "MS-INFO";
    public static final String MSG_REQ = "MS-REQ";
    public static final String MSG_CACHE = "MS-CAC";
    public static final String MSG_DATA = "MS-DATA";
    public static final String MSG_STATUS = "MS-STATUS";
    public static final String MSG_CLOSE = "MS-CLOSE";
    public static final String MSG_MAP = "MS-MAP";
    public static final String MSG_REDUCE = "MS-REDUCE";
    public static final String MSG_BKP = "MS-BACKUP";
    public static final String MSG_REP = "MS-REPLICATE";
}
