package au.com.realestate.hometime;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import au.com.realestate.hometime.models.ApiResponse;
import au.com.realestate.hometime.models.Token;
import au.com.realestate.hometime.models.Tram;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private List<Tram> southTrams;
    private List<Tram> northTrams;
    private int countNorth = 0;
    private int countSouth = 0;

    ListView listView;
    Button btnRefresh, btnClear, btnNorth, btnSouth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);
        btnRefresh = (Button) findViewById(R.id.refreshButton);
        btnClear = (Button) findViewById(R.id.clearButton);
        btnNorth = (Button) findViewById(R.id.northButton);
        btnSouth = (Button) findViewById(R.id.southButton);

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (countNorth == 0 && countSouth == 0)
                {
                    Toast.makeText(MainActivity.this, "Please select a direction first!", Toast.LENGTH_LONG).show();
                }
                else if (countNorth == 1)
                {
                    showNorth();
                }
                else if (countSouth == 1)
                {
                   showSouth();

                }
            }
        });

        btnNorth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countNorth = 1;
                countSouth = 0;
                showNorth();
            }
        });

        btnSouth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countSouth = 1;
                countNorth = 0;
                showSouth();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                northTrams = new ArrayList<>();
                southTrams = new ArrayList<>();
                showTramsNorth();
                showTramsSouth();
                btnNorth.setVisibility(View.VISIBLE);
                btnSouth.setVisibility(View.VISIBLE);
                countNorth = 0;
                countSouth = 0;
            }
        });
    }

    private void showNorth()
    {
        TramsApi tramsApi = createApiClient();
        btnSouth.setVisibility(View.GONE);
        try {
            String token = new RequestToken(tramsApi).execute("").get();
            northTrams = new RequestTrams(tramsApi, token).execute("4055").get();
            showTramsNorth();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        btnNorth.setVisibility(View.VISIBLE);
    }

    private void showSouth()
    {
        TramsApi tramsApi = createApiClient();
        btnNorth.setVisibility(View.GONE);
        try {
            String token = new RequestToken(tramsApi).execute("").get();
            southTrams = new RequestTrams(tramsApi, token).execute("4155").get();
            showTramsSouth();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        btnSouth.setVisibility(View.VISIBLE);
    }

    private void showTramsNorth() {

        listView.setVisibility(View.VISIBLE);
        List<String> northValues = new ArrayList<>();

        for (Tram tram : northTrams) {
            SimpleDateFormat gmtDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            String date= gmtDateFormat.format(dateFromDotNetDate(tram.predictedArrival)).toString();
            String destName = tram.destination.toString();
            String routeNo = tram.routeNo.toString();
            String waitingTime = timeDiffFromDotNetDate(tram.predictedArrival).toString();
            String listDetails = "Destination : "+destName +"\nRoute No : "+ routeNo +"\non "
                    +date+"\nWaiting time in hh:mm:ss is "+ waitingTime;
            northValues.add(listDetails);
        }

        listView.setAdapter(new ArrayAdapter<>(
                this,
                R.layout.list_layout,
                northValues));

    }

    private void showTramsSouth() {

        listView.setVisibility(View.VISIBLE);
        List<String> southValues = new ArrayList<>();

        for (Tram tram : southTrams) {
            SimpleDateFormat gmtDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            String date= gmtDateFormat.format(dateFromDotNetDate(tram.predictedArrival)).toString();
            String destName = tram.destination.toString();
            String routeNo = tram.routeNo.toString();
            String waitingTime = timeDiffFromDotNetDate(tram.predictedArrival).toString();
            String listDetails = "Destination : "+destName +"\nRoute No : "+ routeNo +"\non "
                                    +date+"\nWaiting time in hh:mm:ss is "+ waitingTime ;
            southValues.add(listDetails);
        }

        listView.setAdapter(new ArrayAdapter<>(
                this,
                R.layout.list_layout,
                southValues));
    }

    /////////////
    // Convert .NET Date to Date
    ////////////

    private  String dateExtract(String dotNetDate)
    {
        int startIndex = dotNetDate.indexOf("(") + 1;
        int endIndex = dotNetDate.indexOf("+");
        String date = dotNetDate.substring(startIndex, endIndex);
        return date;
    }

    private Date dateFromDotNetDate(String dotNetDate ) {
        String date = dateExtract(dotNetDate);
        Long unixTime = Long.parseLong(date);
        return new Date(unixTime);
    }

    /////////////
    // Calculate Waiting time for Trams
    ////////////

    private Time timeDiffFromDotNetDate(String dotNetDate) {

        String date = dateExtract(dotNetDate);
        Long unixTime = Long.parseLong(date);
        Date tramArrivalDate = new Date(unixTime);

        Date currentDateTime = new Date();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        currentDateTime = cal.getTime();
        long difference = tramArrivalDate.getTime() - currentDateTime.getTime();
        return new Time(difference);
    }

    ////////////
    // API
    ////////////

    private interface TramsApi {

        @GET("/TramTracker/RestService/GetDeviceToken/?aid=TTIOSJSON&devInfo=HomeTimeAndroid")
        Call<ApiResponse<Token>> token();

        @GET("/TramTracker/RestService//GetNextPredictedRoutesCollection/{stopId}/78/false/?aid=TTIOSJSON&cid=2")
        Call<ApiResponse<Tram>> trams(
                @Path("stopId") String stopId,
                @Query("tkn") String token
        );
    }

    private TramsApi createApiClient() {

        String BASE_URL = "http://ws3.tramtracker.com.au";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build();

        return retrofit.create(TramsApi.class);
    }

    private class RequestToken extends AsyncTask<String, Integer, String> {

        TramsApi api;

        RequestToken(TramsApi api) {
            this.api = api;
        }

        @Override
        protected String doInBackground(String... params) {
            Call<ApiResponse<Token>> call = api.token();
            try {
                return call.execute().body().responseObject.get(0).value;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

   private  class RequestTrams extends AsyncTask<String, Integer, List<Tram>> {

         TramsApi api;
         String token;

        RequestTrams(TramsApi api, String token) {
            this.api = api;
            this.token = token;
        }

        @Override
        protected List<Tram> doInBackground(String... stops) {

            Call<ApiResponse<Tram>> call = api.trams(stops[0], token);
            try {
                Response<ApiResponse<Tram>> resp = call.execute();
                return resp.body().responseObject;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
