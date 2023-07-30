package com.example.covidtracker;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.covidtracker.common.Constants;
import com.example.covidtracker.databinding.ActivityMainBinding;
import com.example.covidtracker.databinding.DialogChooseProvinceBinding;
import com.example.covidtracker.databinding.DialogSettingsBinding;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    DialogChooseProvinceBinding dialogChooseProvinceBinding;
    private TextView totalCasesTextView;
    private TextView totalDeathsTextView;
    private TextView totalRecoveredTextView;
    private DialogSettingsBinding settingsBinding;
    private LineChart chart;
    String[] regions;
    String[] provinces;
    String selectedProvince = "";
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        regions = getResources().getStringArray(R.array.regions);
        totalCasesTextView = binding.totalCases;
        totalDeathsTextView = binding.totalDeaths;
        totalRecoveredTextView = binding.totalRecovered;
        chart = binding.chart;
        provinces = new String[0];
        initListeners();

    }

    private void initListeners() {
        binding.btnFilter.setOnClickListener(v -> {
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
            DialogInterface.OnClickListener dListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        if (dialogChooseProvinceBinding.spinnerProvince.getVisibility() == View.GONE) {
                            return;
                        } else {
                            fetchData();
                            break;
                        }

                    default:
                        dialog.dismiss();
                        break;
                }
            };
            dialogChooseProvinceBinding = DialogChooseProvinceBinding.inflate(getLayoutInflater(), null, false);
            fetchProvince();

            mBuilder.setView(dialogChooseProvinceBinding.getRoot());
            mBuilder.setNegativeButton("Proceed", dListener)
                    .setPositiveButton("Cancel", dListener)
                    .setCancelable(false)
                    .show();
        });
        binding.btnSetting.setOnClickListener(v -> {

            AlertDialog.Builder tBuilder = new AlertDialog.Builder(MainActivity.this);
            settingsBinding = DialogSettingsBinding.inflate(getLayoutInflater(), null, false);
            tBuilder.setView(settingsBinding.getRoot());
            settingsBinding.editIpAddress.setText(Constants.IP);
            DialogInterface.OnClickListener dListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        String editIP = settingsBinding.editIpAddress.getText().toString();
                        if (editIP.equals("")) {
                            Toast.makeText(MainActivity.this, "Failed to save, Please Don't Leave Empty Fields", Toast.LENGTH_SHORT).show();

                        } else {
                            Constants.IP = editIP;
                            alertDialog.dismiss();
                        }

                        break;
                    case DialogInterface.BUTTON_POSITIVE:
                        alertDialog.dismiss();
                        break;
                }
            };

            tBuilder.setNegativeButton("Yes, Proceed", dListener)
                    .setPositiveButton("Cancel", dListener);

            alertDialog = tBuilder.create();
            alertDialog.setCancelable(false);
            alertDialog.show();
        });
    }

    private void fetchProvince() {
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, Constants.provinceURL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null && response.length() > 0) {
                    try {
                        JSONArray array = response.getJSONArray("provinces");
                        provinces = new String[array.length()];
                        for (int x = 0; x < array.length(); x++) {
                            provinces[x] = array.getString(x);
                        }
                    } catch (JSONException e) {

                    }

                    if (provinces.length > 0) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.simple_spinner_item, provinces);

                        // Specify the layout to use when the list of choices appears
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                        // Apply the adapter to the spinner
                        dialogChooseProvinceBinding.spinnerProvince.setAdapter(adapter);
                        dialogChooseProvinceBinding.spinnerProvince.setVisibility(View.VISIBLE);
                        dialogChooseProvinceBinding.lblChooseProvince.setVisibility(View.VISIBLE);

                        dialogChooseProvinceBinding.spinnerProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                selectedProvince = provinces[position];
                                if (selectedProvince != "") {
                                    binding.txtProvince.setText(String.format("Province: %s", selectedProvince));

                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                    }
                }
            }
        }, error -> {
            if (error != null) {
                Log.e("fetchProvince", error.getLocalizedMessage());
            }
        });

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(objectRequest);
    }

    private void fetchData() {
        String url = "";
        if (selectedProvince != "") {
            url = String.format(Constants.covidURL + "?province=%s", selectedProvince);
        } else {
            url = Constants.covidURL;
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.e("DATA", response.toString());
                    try {
                        JSONArray data = response.getJSONArray("data");
                        List<Entry> entries = new ArrayList<>();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject dayData = data.getJSONObject(i);
                            int cases = dayData.getInt("cases");
                            entries.add(new Entry(i, cases));
                        }

                        LineDataSet dataSet = new LineDataSet(entries, "Total Cases");
                        dataSet.setDrawValues(false);
                        dataSet.setColor(Color.BLUE);
                        dataSet.setCircleColor(Color.BLUE);

                        LineData lineData = new LineData(dataSet);
                        chart.setData(lineData);
                        chart.invalidate();
//
//                            // Set custom X-axis labels
                        XAxis xAxis = chart.getXAxis();
                        xAxis.setValueFormatter(new ValueFormatter() {
                            @Override
                            public String getAxisLabel(float value, AxisBase axis) {
                                int index = (int) value;
                                try {
                                    if (index < data.length()) {
                                        String date = "";
                                        try {
                                            date = data.getJSONObject(index).getString("date");
                                        } catch (JSONException e) {
                                            date = "";
                                        } catch (Exception e2) {
                                            date = "";

                                        }
                                        // Modify the date format according to your needs
                                        // You may want to use a SimpleDateFormat or another library for this
                                        return date;
                                    }
                                } catch (Exception e) {
                                    Log.e("getAxisLabel_err", e.getLocalizedMessage());
                                }
                                return "";
                            }
                        });
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        xAxis.setGranularity(1f);

                        // Disable right Y-axis
                        YAxis rightYAxis = chart.getAxisRight();
                        rightYAxis.setEnabled(false);

                        // Set custom Y-axis labels
                        YAxis leftYAxis = chart.getAxisLeft();
                        leftYAxis.setValueFormatter(new ValueFormatter() {
                            @Override
                            public String getAxisLabel(float value, AxisBase axis) {
                                return String.valueOf((int) value);
                            }
                        });

                        // Update total cases, total deaths, and total recovered
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            int totalCases = obj.getInt("cases");
                            int totalDeaths = obj.getInt("died");
                            int totalRecovered = obj.getInt("recovered");

                            binding.txtTodayCases.setText(String.format("Today Cases: %s", 0));
                            totalCasesTextView.setText(String.format("Total Cases: %s", totalCases));
                            totalDeathsTextView.setText(String.format("Total Deaths: %s", totalDeaths));
                            totalRecoveredTextView.setText(String.format("Total Recovered: %s", totalRecovered));
                            break;
                        }

                    } catch (JSONException e) {
                        Log.e("error", e.getLocalizedMessage());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show();
                    }
                });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }
}

