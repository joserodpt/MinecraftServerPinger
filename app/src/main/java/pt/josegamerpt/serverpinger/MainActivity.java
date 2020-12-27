package pt.josegamerpt.serverpinger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.thekhaeng.pushdownanim.PushDownAnim;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;
import needle.Needle;
import needle.UiRelatedTask;

public class MainActivity extends AppCompatActivity {

    SweetAlertDialog pinging;
    JSONObject get;
    Drawable icon;

    TextInputEditText ip = null;
    TextInputEditText port = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkDark(getResources().getConfiguration());

        ip = findViewById(R.id.ipinput);
        port = findViewById(R.id.portinput);

        TextView t = findViewById(R.id.replyJson);
        t.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("jsonping", t.getText());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getApplication(), "JSON data copied to clipboard!",
                    Toast.LENGTH_SHORT).show();
            return false;
        });

        TextView tfoot = findViewById(R.id.footer);
        tfoot.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme);
            builder.setTitle("About this app");
            builder.setMessage("Developed by José Rodrigues\nSource code available on Github\nCopyright 2020");
            builder.setCancelable(false);

            String positiveText = getString(android.R.string.ok);
            builder.setPositiveButton(positiveText,
                    (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();
        });

        TextView tm = findViewById(R.id.mods);
        tm.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("modslist", tm.getText());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getApplication(), "Mods list copied to clipboard!",
                    Toast.LENGTH_SHORT).show();
            return false;
        });

        PushDownAnim.setPushDownAnimTo(findViewById(R.id.go)).setOnClickListener(v -> {
            pinging = new SweetAlertDialog(findViewById(R.id.main).getContext(), SweetAlertDialog.PROGRESS_TYPE);
            pinging.getProgressHelper().setBarColor(Color.BLUE);
            pinging.setTitleText(getApplicationContext().getString(R.string.app_gettinginfo));
            pinging.setCancelable(false);
            pinging.show();

            //makeURL
            final String link = "https://api.mcsrvstat.us/2/" + ip.getText() + ":" + port.getText();

            Needle.onBackgroundThread().execute(new UiRelatedTask() {
                @Override
                protected Object doWork() {
                    return getInfoFromAPI(link);
                }

                @Override
                protected void thenDoUiRelatedWork(Object o) {
                    TextView t1 = findViewById(R.id.replyJson);
                    try {
                        get = new JSONObject(o.toString());
                        t1.setText(get.toString());
                        if (!get.getBoolean("online")) {

                            String notfound = getApplicationContext().getString(R.string.server_not_found);
                            final SweetAlertDialog off = new SweetAlertDialog(findViewById(R.id.main).getContext(), SweetAlertDialog.WARNING_TYPE);
                            off.setTitleText(getApplicationContext().getString(R.string.server_offline));
                            off.setConfirmButton(getApplicationContext().getString(android.R.string.ok), sweetAlertDialog -> {
                                off.dismissWithAnimation();
                                pinging.dismissWithAnimation();
                            });
                            off.show();

                            TextView slots = findViewById(R.id.slots);
                            slots.setText(notfound);
                            TextView ver = findViewById(R.id.version);
                            ver.setText(notfound);
                            TextView prot = findViewById(R.id.protocol);
                            prot.setText(notfound);
                            TextView motd = findViewById(R.id.motd);
                            motd.setText(notfound);
                            findViewById(R.id.playerdata).setVisibility(View.GONE);
                            findViewById(R.id.modsdata).setVisibility(View.GONE);
                            ImageView v = findViewById(R.id.servericon);
                            v.setImageDrawable(getApplicationContext().getDrawable(R.drawable.unknownsrvr));

                            //TODO:

                        } else {
                            sucess();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        final SweetAlertDialog off = new SweetAlertDialog(findViewById(R.id.main).getContext(), SweetAlertDialog.ERROR_TYPE);
                        off.setTitleText(getApplicationContext().getString(R.string.get_error) + e.getMessage());
                        off.setConfirmButton(getApplicationContext().getString(android.R.string.ok), sweetAlertDialog -> {
                            off.dismissWithAnimation();
                            pinging.dismissWithAnimation();
                        });
                        off.setCancelable(false);
                        off.show();
                    }

                }
            });
        });
    }

    public void checkDark(Configuration config)
    {
        int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                // Night mode is not active, we're using the light theme
                AppUtils.setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                getWindow().setStatusBarColor(Color.WHITE);
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setNavigationBarColor(Color.WHITE);
                }
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                // Night mode is active, we're using dark theme
                AppUtils.setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
                getWindow().setStatusBarColor(Color.BLACK);
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setNavigationBarColor(Color.BLACK);
                }
                break;
        }
    }

    //dark mode support
    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        checkDark(config);
    }

    //sucess ping
    private void sucess() throws JSONException {
        //icon

        Needle.onBackgroundThread().execute(new UiRelatedTask() {
            @Override
            protected Object doWork() {
                InputStream is = null;
                try {
                    is = (InputStream) new URL("https://api.mcsrvstat.us/icon/" + ip.getText() + ":" + port.getText()).getContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                icon = Drawable.createFromStream(is, "serverIcon");
                return null;
            }

            @Override
            protected void thenDoUiRelatedWork(Object o) {
                ImageView v = findViewById(R.id.servericon);
                v.setImageDrawable(icon);
                v.setBackground(null);
            }
        });

        //saveLastSucessfull IP and PORT

        //slots and version
        TextView slots = findViewById(R.id.slots);
        TextView ver = findViewById(R.id.version);
        TextView prot = findViewById(R.id.protocol);


        JSONObject players = get.getJSONObject("players");

        slots.setText(getApplicationContext().getString(R.string.name_players) + players.getInt("online")
                + "/" + players.getInt("max"));
        ver.setText(getApplicationContext().getString(R.string.name_version) + get.getString("version"));
        prot.setText(getApplicationContext().getString(R.string.name_protocol) + get.getString("protocol"));

        //motd
        TextView motd = findViewById(R.id.motd);

        JSONObject mtd = get.getJSONObject("motd");

        switch (mtd.getJSONArray("html").length()) {
            case 1:
                motd.setText(HtmlCompat.fromHtml(mtd.getJSONArray("html").get(0).toString().replaceAll("^\\s+", "").replaceAll("\\s+$", ""), HtmlCompat.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);
                break;
            case 2:
                motd.setText(HtmlCompat.fromHtml(mtd.getJSONArray("html").get(0).toString().replaceAll("^\\s+", "").replaceAll("\\s+$", "") + "\n" + mtd.getJSONArray("html").get(1).toString().replaceAll("^\\s+", "").replaceAll("\\s+$", ""), HtmlCompat.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);
                break;
            default:
                motd.setText(getApplicationContext().getString(R.string.no_server_mod));
                break;
        }

        //players
        if (players.has("list")) {
            TextView textplayerlist = findViewById(R.id.players);

            ArrayList<String> playerList = new ArrayList<>();
            JSONArray playerListArray = players.getJSONArray("list");
            for (int i = 0; i < playerListArray.length(); i++) {
                playerList.add(playerListArray.get(i).toString());
            }

            textplayerlist.setText(StringUtils.join(playerList, "\n"));

            findViewById(R.id.playerdata).setVisibility(View.VISIBLE);
            //has player list
        } else {
            findViewById(R.id.playerdata).setVisibility(View.GONE);
        }

        //mods

        if (get.has("mods")) {

            ArrayList<String> modsList = new ArrayList<>();
            JSONObject mods = get.getJSONObject("mods");

            JSONArray modslistarray = mods.getJSONArray("names");
            for (int i = 0; i < modslistarray.length(); i++) {
                modsList.add(modslistarray.get(i).toString());
            }
            TextView modstext = findViewById(R.id.mods);

            modstext.setText(StringUtils.join(modsList, "\n"));

            findViewById(R.id.modsdata).setVisibility(View.VISIBLE);
            //has player list
        } else {
            findViewById(R.id.modsdata).setVisibility(View.GONE);
        }

        pinging.dismissWithAnimation();
    }

    //retrieve info from api
    public Object getInfoFromAPI(String link) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(link);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();


            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuilder buffer = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            return buffer.toString();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}