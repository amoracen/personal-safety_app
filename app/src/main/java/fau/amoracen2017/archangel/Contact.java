package fau.amoracen2017.archangel;
import androidx.annotation.NonNull;
/**
 * Class to represent the user's contacts
 * @author Alicia Mitchell
 */
public class Contact {
    private String ContactName1;
    private String PhoneNumber1;
    private String ContactName2;
    private String PhoneNumber2;

    /**
     * Default Constructor
     */
 public Contact(){
    }

    /**
     * Parameterized constructor
     * @param ContactName1 a string representing the first contact' name
     * @param PhoneNumber1 a string representing the first contact' phone number
     * @param ContactName2 a string representing the second contact' name
     * @param PhoneNumber2 a string representing the second contact' phone number
     */
    public Contact( String ContactName1, String PhoneNumber1,String ContactName2, String PhoneNumber2) {
        this.ContactName1 = ContactName1;
        this.PhoneNumber1 = PhoneNumber1;
        this.ContactName2 = ContactName2;
        this.PhoneNumber2 = PhoneNumber2;
    }

    public String getContactName1() {
        return ContactName1;
    }

    public String getPhoneNumber1() {
        return PhoneNumber1;
    }
    public String getContactName2() {
        return ContactName2;
    }

    public String getPhoneNumber2() {
        return PhoneNumber2;
    }
    public void setContactName1(String ContactName1) {
        this.ContactName1 = ContactName1;
    }

    public void setPhoneNumber1(String PhoneNumber1) {
        this.PhoneNumber1 = PhoneNumber1;
    }
    public void setContactName2(String ContactName2) {
        this.ContactName2 = ContactName2;
    }

    public void setPhoneNumber2(String PhoneNumber2) {
        this.PhoneNumber2 = PhoneNumber2;
    }

    @NonNull
    @Override
    public String toString() {
        String info = "";
        info += " Contact Name1: " + this.ContactName1;
        info += " Phone Number1: : " + this.PhoneNumber1;
        info += " Contact Name2: " + this.ContactName2;
        info += " Phone Number2: " + this.PhoneNumber2;
        return info;
    }
}
