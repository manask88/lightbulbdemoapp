/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package com.example.manuel.lightbulb;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class SpeechRecognizerManager {

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "ok light";
    private edu.cmu.pocketsphinx.SpeechRecognizer mPocketSphinxRecognizer;
    private static final String TAG = SpeechRecognizerManager.class.getSimpleName();
    protected Intent mSpeechRecognizerIntent;
    protected android.speech.SpeechRecognizer mGoogleSpeechRecognizer;
    private Context mContext;
    private OnResultListener mOnResultListener;


    public SpeechRecognizerManager(Context context) {
        this.mContext = context;
        initPockerSphinx();
        initGoogleSpeechRecognizer();

    }


    private void initPockerSphinx() {

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(mContext);

                    //Performs the synchronization of assets in the application and external storage
                    File assetDir = assets.syncAssets();

                    //Creates a new SpeechRecognizer builder with a default configuration
                    SpeechRecognizerSetup speechRecognizerSetup = defaultSetup();

                    //Set Dictionary and Acoustic Model files
                    speechRecognizerSetup.setAcousticModel(new File(assetDir, "en-us-ptm"));
                    speechRecognizerSetup.setDictionary(new File(assetDir, "cmudict-en-us.dict"));

                    // Threshold to tune for keyphrase to balance between false positives and false negatives
                    speechRecognizerSetup.setKeywordThreshold(1e-45f);

                    //Creates a new SpeechRecognizer object based on previous set up.
                    mPocketSphinxRecognizer = speechRecognizerSetup.getRecognizer();

                    mPocketSphinxRecognizer.addListener(new PocketSphinxRecognitionListener());

                    // Create keyword-activation search.
                    mPocketSphinxRecognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    Toast.makeText(mContext, "Failed to init mPocketSphinxRecognizer ", Toast.LENGTH_SHORT).show();
                } else {
                    restartSearch(KWS_SEARCH);
                }
            }
        }.execute();

    }

    private void initGoogleSpeechRecognizer() {

        mGoogleSpeechRecognizer = android.speech.SpeechRecognizer
                .createSpeechRecognizer(mContext);

        mGoogleSpeechRecognizer.setRecognitionListener(new GoogleRecognitionListener());

        mSpeechRecognizerIntent = new Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        mSpeechRecognizerIntent.putExtra( RecognizerIntent. EXTRA_CONFIDENCE_SCORES, true);
    }


    public void destroy() {
        if (mPocketSphinxRecognizer != null) {
            mPocketSphinxRecognizer.cancel();
            mPocketSphinxRecognizer.shutdown();
            mPocketSphinxRecognizer = null;
        }


        if (mGoogleSpeechRecognizer != null) {
            mGoogleSpeechRecognizer.cancel();
            ;
            mGoogleSpeechRecognizer.destroy();
            mPocketSphinxRecognizer = null;
        }

    }

    private void restartSearch(String searchName) {

        mPocketSphinxRecognizer.stop();

        mPocketSphinxRecognizer.startListening(searchName);

    }


    protected class PocketSphinxRecognitionListener implements edu.cmu.pocketsphinx.RecognitionListener {

        @Override
        public void onBeginningOfSpeech() {
        }


        /**
         * In partial result we get quick updates about current hypothesis. In
         * keyword spotting mode we can react here, in other modes we need to wait
         * for final result in onResult.
         */
        @Override
        public void onPartialResult(Hypothesis hypothesis) {
            if (hypothesis == null)
            {
                Log.d(TAG,"null");


                return;

            }


            String text = hypothesis.getHypstr();
            Log.d(TAG,text);
            if (text.equals(KEYPHRASE)) {
                mGoogleSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                mPocketSphinxRecognizer.cancel();
                Toast.makeText(mContext, "You said: "+text, Toast.LENGTH_SHORT).show();

            }
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
        }


        /**
         * We stop mPocketSphinxRecognizer here to get a final result
         */
        @Override
        public void onEndOfSpeech() {

        }

        public void onError(Exception error) {
        }

        @Override
        public void onTimeout() {
        }

    }


    protected class GoogleRecognitionListener implements
            android.speech.RecognitionListener {

        private final String TAG = GoogleRecognitionListener.class
                .getSimpleName();

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onError(int error) {
            Log.e(TAG, "onError:" + error);

            mPocketSphinxRecognizer.startListening(KWS_SEARCH);


        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResultsheard:");

        }

        @Override
        public void onResults(Bundle results) {
            if ((results != null)
                    && results
                    .containsKey(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)) {
                ArrayList<String> heard = results
                        .getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                float[] scores = results
                        .getFloatArray(android.speech.SpeechRecognizer.CONFIDENCE_SCORES);

                for (int i = 0; i < heard.size(); i++) {
                    Log.d(TAG, "onResultsheard:" + heard.get(i)
                            + " confidence:" + scores[i]);

                }


                //send list of words to activity
                if (mOnResultListener!=null){
                    mOnResultListener.OnResult(heard);
                }

            }

            mPocketSphinxRecognizer.startListening(KWS_SEARCH);


        }


        @Override
        public void onEvent(int eventType, Bundle params) {

        }

    }

    public void setOnResultListner(OnResultListener onResultListener){
        mOnResultListener=onResultListener;
    }

    public interface OnResultListener
    {
        public void OnResult(ArrayList<String> commands);
    }
}
