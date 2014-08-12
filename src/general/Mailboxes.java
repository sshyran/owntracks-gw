/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package general;

/**
 *
 * @author christoph
 */
public class Mailboxes {
    public static String UnderVoltage = "UnderVoltage";

    final private int NUMBEROFMAILBOXES = 11;
    final private int SIZEOFMAILBOXES = 20;
    Mailbox[] mailboxes;
    
    private Mailboxes() {
        mailboxes = new Mailbox[NUMBEROFMAILBOXES];
        for (int i = 0; i < NUMBEROFMAILBOXES; i++) {
            mailboxes[i] = new Mailbox(SIZEOFMAILBOXES);
        }
    }
    
    public static Mailboxes getInstance() {
        return MailboxesHolder.INSTANCE;
    }
    
    public static Mailbox getInstance(int index) {
        Mailboxes mailboxes = Mailboxes.getInstance();
        if (index < mailboxes.NUMBEROFMAILBOXES) {
            return mailboxes.mailboxes[index];
        } else {
            return null;
        }
    }
    
    private static class MailboxesHolder {

        private static final Mailboxes INSTANCE = new Mailboxes();
    }
}
