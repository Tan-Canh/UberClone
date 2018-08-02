package canh.tan.nguye.uberclone.common;

import canh.tan.nguye.uberclone.interfaces.IGoogleAPI;
import canh.tan.nguye.uberclone.remote.RetrofitClient;

public class Common {
    private static final String baseURL = "https://maps.googleapis.com/";

    public static IGoogleAPI getGoogleAPI(){
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
}
