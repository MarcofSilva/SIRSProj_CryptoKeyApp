package a46.cryptokey;
//TODO pensar qual a melhor distribuicao por packages

public class TOTP extends HOTP{

    private int _timeRangeOfPassword; //One time password update every TIME_RANGE_PASSWORD seconds

    public TOTP(int numberDigitsOTP, int timeRangeOfPassword) {
        super(numberDigitsOTP);
        _timeRangeOfPassword = timeRangeOfPassword;
    }

    public String generateOTP() {
        long counter = getUnixTimeCounter();
        return super.generateOTP(counter);
    }

    public long getUnixTimeCounter() {
        long unixTime = System.currentTimeMillis()/1000L;
        return unixTime / _timeRangeOfPassword;
    }
}
