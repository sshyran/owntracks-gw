package general;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author christoph
 */
public class SemAT {
    
    private PrioSem prioSem;
    
    private SemAT() {
        this.prioSem = new PrioSem(0);
    }
    
    public static PrioSem getInstance() {
        SemAT semAT = SemAT.getSemATInstance();
        return semAT.prioSem;
    }
    
    public static SemAT getSemATInstance() {
        return SemATHolder.INSTANCE;
    }
    
    private static class SemATHolder {

        private static final SemAT INSTANCE = new SemAT();
    }
}
