package com.dyejeekis.quotesapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    @BindView(R.id.textView_quote) TextView textQuote;
    @BindView(R.id.textView_author) TextView textAuthor;
    @BindView(R.id.fab_share) FloatingActionButton fabShare;
    @BindView(R.id.button_next) Button buttonNext;
    @BindView(R.id.button_previous) Button buttonPrevious;
    @BindView(R.id.checkBox_daily_quotes) CheckBox checkBoxDaily;
    @BindView(R.id.progressBar) ProgressBar progressBar;
    @BindView(R.id.adView) AdView adView;

    private OkHttpClient okHttpClient;
    private int currentKey;
    private List<Quote> quoteList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, getResources().getString(R.string.admob_app_id));
        ButterKnife.bind(this);

        fabShare.setOnClickListener(this);
        buttonNext.setOnClickListener(this);
        buttonPrevious.setOnClickListener(this);
        checkBoxDaily.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dailyQuotes", false));
        checkBoxDaily.setOnCheckedChangeListener(this);
        adView.loadAd(new AdRequest.Builder().build());

        okHttpClient = new OkHttpClient();
        quoteList = new ArrayList<>();
        currentKey = 0;
        randomQuote();
    }

    public void addQuote(final  Quote quote) {
        quoteList.add(quote);
        currentKey = quoteList.size()-1;
        updateQuoteMainThread(getCurrentQuote());
    }

    /**
     * only run on ui thread
     * @param newQuote
     */
    public void updateQuote(final Quote newQuote) {
        setProgressBarVisible(false);
        textQuote.setText(newQuote.getQuoteText());
        SpannableString authorSpannable = new SpannableString(newQuote.getQuoteAuthor());
        authorSpannable.setSpan(new UnderlineSpan(), 0, newQuote.getQuoteAuthor().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        textAuthor.setText(authorSpannable);
        textAuthor.setOnClickListener(MainActivity.this);
        buttonPrevious.setEnabled(currentKey > 0);
    }

    public void updateQuoteMainThread(final Quote newQuote) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateQuote(newQuote);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.fab_share) {
            startActivity(getCurrentQuote().getShareIntent());
        }
        else if(v.getId() == R.id.textView_author) {
            try {
                startActivity(getCurrentQuote().getAuthorWikiIntent());
            } catch (Exception e) {
                e.printStackTrace();
                Util.displayShortToast(this, "Error creating wiki URL");
            }
        }
        else if(v.getId() == R.id.button_next) {
            if(currentKey == quoteList.size()-1 || quoteList.isEmpty()) {
                randomQuote();
            }
            else if(!quoteList.isEmpty()){
                nextQuote();
            }
        }
        else if(v.getId() == R.id.button_previous) {
            previousQuote();
        }
    }

    private void nextQuote() {
        currentKey++;
        updateQuote(quoteList.get(currentKey));
    }

    private void previousQuote() {
        if(currentKey > 0) {
            currentKey--;
            updateQuote(quoteList.get(currentKey));
        }
    }

    private void randomQuote() {
        setProgressBarVisible(true);
        QuoteRetrieval.randomQuote(this, okHttpClient);
    }

    /**
     * only run on ui thread
     * @param flag
     */
    public void setProgressBarVisible(boolean flag) {
        progressBar.setVisibility(flag ? View.VISIBLE : View.GONE);
    }

    private Quote getCurrentQuote() {
        return quoteList.get(currentKey);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(buttonView.getId() == R.id.checkBox_daily_quotes) {
            Util.setDailyQuotesActive(this, isChecked);
        }
    }

}
