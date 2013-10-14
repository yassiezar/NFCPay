/*
 * Copyright 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skripsie.nfcpay6;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity 
{
    private static final String DEBUG_TAG = "example";
    
    private boolean mResumed = false;
    private boolean mWriteMode = false;
    
    private String userName;
    private String userPass;
    
    NfcAdapter mNfcAdapter;
    TextView mNote;
    TextView mTextView;

    PendingIntent mNfcPendingIntent;
    IntentFilter[] mWriteTagFilters;
    IntentFilter[] mNdefExchangeFilters;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        setContentView(R.layout.activity_main);
        
        findViewById(R.id.coke_button).setOnClickListener(mTagWriter);
        findViewById(R.id.choc_button).setOnClickListener(mTagWriter);
        findViewById(R.id.lays_button).setOnClickListener(mTagWriter);
        
        mNote = ((TextView) findViewById(R.id.edit_text));
        //mNote.setVisibility(View.GONE);
        mNote.addTextChangedListener(mTextWatcher);
        
        mTextView = (TextView)findViewById(R.id.text_view);
        mTextView.setText("Please select your favourite weakness.");
        
        //Get user login details
        userName = ((GlobalVar) this.getApplication()).getUserName();
        userPass = ((GlobalVar) this.getApplication()).getUserPassword();
        //Toast.makeText(this, userPass, Toast.LENGTH_SHORT).show();

        // Handle all of our received NFC intents in this activity.
        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Intent filters for reading a note from a tag or exchanging over p2p.
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try 
        {
            ndefDetected.addDataType("text/plain");
        }
        catch (MalformedMimeTypeException e) { }
        
        mNdefExchangeFilters = new IntentFilter[] { ndefDetected };

        // Intent filters for writing to a tag
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mWriteTagFilters = new IntentFilter[] { tagDetected };
    }

    @Override
    protected void onResume() 
    {
        super.onResume();
        mResumed = true;
        // Sticky notes received from Android
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) 
        {
            setIntent(new Intent()); // Consume this intent.
        }
        enableNdefExchangeMode();
    }

    @Override
    protected void onPause() 
    {
        super.onPause();
        mResumed = false;
        mNfcAdapter.disableForegroundNdefPush(this);
    }

    @Override
    protected void onNewIntent(Intent intent) 
    {
        // NDEF exchange mode
        /*if (!mWriteMode && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) 
        {
            //NdefMessage[] msgs = getNdefMessages(intent);
            //promptForContent(msgs[0]);
        }*/

        // Tag writing mode
        if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) 
        {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            writeTag(getNoteAsNdef(), detectedTag);
        }
    }

    private TextWatcher mTextWatcher = new TextWatcher() 
    {

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) 
        {

        }

        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) 
        {

        }

        @Override
        public void afterTextChanged(Editable arg0) 
        {
            if (mResumed) 
            {
                mNfcAdapter.enableForegroundNdefPush(MainActivity.this, getNoteAsNdef());
            }
        }
    };

    private View.OnClickListener mTagWriter = new View.OnClickListener() 
    {
        @Override
        public void onClick(View arg0) 
        {
            // Write to a tag for as long as the dialog is shown.
            disableNdefExchangeMode();
            enableTagWriteMode();
            
            String prod_code = "";
            
            //Check network connectivity 
            ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            
			if(networkInfo != null && networkInfo.isConnected()) 
			{
	            if(arg0.getId() == R.id.coke_button)
	            {
	            	prod_code = "A061";
	            }
	            
	            if(arg0.getId() == R.id.lays_button)
	            {
	            	prod_code = "233C";
	            }
	            
	            if(arg0.getId() == R.id.choc_button)
	            {
	            	prod_code = "4FF1";
	            }
	            	
	            try
            	{
	            	new DownloadWebpageTask().execute("http://ec2-54-213-127-119.us-west-2.compute.amazonaws.com/nfc/?user=" + encrypt_rsa(userName) + "&password=" + encrypt_rsa(userPass) + "&product=" + encrypt_rsa(prod_code));
            		mNote.setText("Contacting server...");
            	}
            	
            	catch(Exception e)
            	{
            		mNote.setText("fail");
            	}
	        }
			
			else
			{
				mNote.setText("No network");
			}
        }
    };

    private NdefMessage getNoteAsNdef() 
    {
        byte[] textBytes = mNote.getText().toString().getBytes();
        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain".getBytes(),
                new byte[] {}, textBytes);
        return new NdefMessage(new NdefRecord[] { textRecord });
    }

    private void enableNdefExchangeMode() 
    {
        mNfcAdapter.enableForegroundNdefPush(MainActivity.this, getNoteAsNdef());
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mNdefExchangeFilters, null);
    }

    private void disableNdefExchangeMode() 
    {
        mNfcAdapter.disableForegroundNdefPush(this);
        mNfcAdapter.disableForegroundDispatch(this);
    }

    private void enableTagWriteMode() 
    {
        mWriteMode = true;
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mWriteTagFilters = new IntentFilter[] { tagDetected };
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
        
        mNote.setText("");
    }

/*    private void disableTagWriteMode() 
    {
        mWriteMode = false;
        mNfcAdapter.disableForegroundDispatch(this);
    }*/

    boolean writeTag(NdefMessage message, Tag tag) 
    {
        int size = message.toByteArray().length;

        try 
        {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) 
            {
                ndef.connect();

                if (!ndef.isWritable()) 
                {
                    toast("Tag is read-only.");
                    return false;
                }
                if (ndef.getMaxSize() < size) 
                {
                    toast("Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size
                            + " bytes.");
                    return false;
                }

                ndef.writeNdefMessage(message);

                toast("Wrote message to pre-formatted tag.");
                return true;
            }
            
            else 
            {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) 
                {
                    try 
                    {
                        format.connect();
                        format.format(message);
                        toast("Formatted tag and wrote message");
                        return true;
                    } 
                    
                    catch (IOException e) 
                    {
                        toast("Failed to format tag.");
                        return false;
                    }
                } 
                
                else 
                {
                    toast("Tag doesn't support NDEF.");
                    return false;
                }
            }
        } catch (Exception e) 
        {
            toast("Failed to write tag");
        }

        return false;
    }

    private void toast(String text) 
    {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
    
    private class DownloadWebpageTask extends AsyncTask<String, Void, String> 
	{
        @Override
        protected String doInBackground(String... urls) 
        {
              
            // params comes from the execute() call: params[0] is the url.
            try 
            {
                return downloadUrl(urls[0]);
            } 
            
            catch (IOException e) 
            {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) 
        {
        	//mNote.setVisibility(View.GONE);
        	mTextView.setText("Please swipe your phone across the receiver.");
        	mNote.setText(result);

//        	if(result.equals("1"))
//        	{
//                mTextView.setText("Your transation has been approved. Please swipe your phone across the NFC receiver.");
//        		mNote.setText("1");			//verander hier
//        	}
//        	
//        	else
//        	{
//        		mTextView.setText("You don't have enough money. Please load some more on at this adress: http://ec2-54-213-127-119.us-west-2.compute.amazonaws.com/user_admin");
//        		mNote.setText("0");
//        	}
        }
        
        private String downloadUrl(String myurl) throws IOException 
        {
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 500;
                
            try 
            {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                Log.d(DEBUG_TAG, "The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                String contentAsString = readIt(is, len);
                return contentAsString;
                
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
            } 
            
            finally 
            {
                if (is != null) 
                {
                    is.close();
                } 
            }
        }
        
        public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException 
        {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");        
            char[] buffer = new char[len];
            reader.read(buffer);
            return new String(buffer);
        }
    }
	
	public String encrypt_rsa(String original) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
	{
		InputStream is = getResources().openRawResource(R.raw.public_key);
	    DataInputStream dis = new DataInputStream(is);
	    byte [] keyBytes = new byte [(int) is.available()];
	    dis.readFully(keyBytes);
	    dis.close();

	    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
	    KeyFactory kf = KeyFactory.getInstance("RSA");
	    
	    PublicKey public_key = kf.generatePublic(spec);
	    
	    byte[] encryptedBytes;
	    
	    Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
	    
	    cipher.init(Cipher.ENCRYPT_MODE, public_key);
	    encryptedBytes = cipher.doFinal(original.getBytes());
	    String encryptedInString = new String(Base64Coder.encode(encryptedBytes));
	    
	    return encryptedInString;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.layout.menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
	        case R.id.settings:
	        	Intent intent = new Intent(this, ChangeSettings.class);
				startActivity(intent);
				
				finish();
				
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
		}
	}
}