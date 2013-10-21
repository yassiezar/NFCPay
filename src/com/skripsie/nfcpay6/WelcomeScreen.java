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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WelcomeScreen extends Activity implements OnClickListener
{
	private static final String DEBUG_TAG = "example";
	
	private Button nextButton;
	private Button readDb;
	
	private TextView textView;
	
	private EditText nameText;
	private EditText passText;
	
	private CheckBox newUserCheckbox;
	
	private boolean newUserChecked = false;
	
	private SQLiteDatabase myDatabase;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome_screen);
		
		nextButton = (Button)findViewById(R.id.next_screen_button);
		nextButton.setOnClickListener(this);
		
		textView = (TextView)findViewById(R.id.welcome_text);
		
		nameText = (EditText)findViewById(R.id.name_text);
		passText = (EditText)findViewById(R.id.password_text);
		
		newUserCheckbox = (CheckBox)findViewById(R.id.new_user_checkbox);
		
		boolean firstboot = getSharedPreferences("BOOT_PREF", MODE_PRIVATE).getBoolean("firstboot", true);
		
		if(firstboot)
		{
			getSharedPreferences("BOOT_PREF", MODE_PRIVATE).edit().putBoolean("firstboot", false).commit();
			
			myOpenOrCreateDatabase();
			
			//finish();
		}
		
		else
		{
			readUserData();
			
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			
			finish();
		}
	}
	
	public void onCheckboxClicked(View view)
	{
		boolean checked = ((CheckBox) view).isChecked();
		
		switch(view.getId())
		{
			case R.id.new_user_checkbox:
				if(checked)
				{
					newUserChecked = true;
				}
				break;
		}
	}
	
	public void myOpenOrCreateDatabase()
	{
    	String tableName = "userTable";
    	
    	try
    	{
    		myDatabase = this.openOrCreateDatabase("UserDatabase", MODE_PRIVATE, null);
    		myDatabase.execSQL("CREATE TABLE IF NOT EXISTS "
				     + tableName
				     + " (NameField VARCHAR, PassField VARCHAR, AgeField INT(3));");
    		
    		myDatabase.execSQL("INSERT INTO "
		     + tableName
		     + " (NameField, PassField, AgeField)"
		     + " VALUES (' ', ' ', 0);");
    	}
    	
    	catch(Exception e)
    	{
    		Toast.makeText(this, "Could not create db", Toast.LENGTH_SHORT).show();
    	}
    	
    	finally
    	{
    		
    	}
	}
	
	public boolean saveUserData()
	{
		String TableName = "userTable";
		String userName = null;
		String userPass = null;
		
		try
		{
			userName = nameText.getText().toString();
			userPass = passText.getText().toString();
			
			if(userName == null)
			{
				Toast.makeText(this, "Invalid username", Toast.LENGTH_SHORT).show();
				return false;
			}
			
			if(userPass == null)
			{
				Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show();
				return false;
			}
			
			myDatabase.execSQL("UPDATE " + TableName
			 + " SET NameField='" + userName + "', PassField='" + userPass + "';");
			
			((GlobalVar) this.getApplication()).setUserName(userName);
			((GlobalVar) this.getApplication()).setUserPassword(userPass);
			
			Toast.makeText(this, "Data saved", Toast.LENGTH_SHORT).show();
		}
		
		catch(Exception e)
		{
			Toast.makeText(this, "Could not save data", Toast.LENGTH_SHORT).show();
		}
		
		finally
		{
			
		}
		
		return true;
	}
	
	public void readUserData()
	{
		String TableName = "userTable";
		String userName = null;
		String userPass = null;
		String data = "";
		
		try
		{
			//Retrieve from database
			myDatabase = this.openOrCreateDatabase("UserDatabase", MODE_PRIVATE, null);
			Cursor c = myDatabase.rawQuery("SELECT * FROM " + TableName + " ", null);
			
			int Column1 = c.getColumnIndex("NameField");
			int Column2 = c.getColumnIndex("PassField");
			
			c.moveToFirst();

			if (c != null)
			{
				do
				{
					userName = c.getString(Column1);
				    userPass = c.getString(Column2);
				    data = data + userPass + "/" + userName + "\n";
				}
				while(c.moveToNext());
			}
			
			((GlobalVar) this.getApplication()).setUserName(userName);
			((GlobalVar) this.getApplication()).setUserPassword(userPass);
		}
		
		catch(Exception e)
		{
			Toast.makeText(this, "Could not read data", Toast.LENGTH_SHORT).show();
		}
		
		finally
		{
			
		}
	}
	
	public void onClick(View view)
	{
		ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
       
		if(view.getId() == R.id.next_screen_button)
		{
			if(newUserChecked == true && networkInfo != null && networkInfo.isConnected())
			{
				try
				{
					new DownloadWebpageTask().execute("http://ec2-54-213-127-119.us-west-2.compute.amazonaws.com/add_nfc_user/?username=" + "awe2" + "&password=" + "meme" + "&email=");
				}
				
				catch(Exception e)
				{
					
				}
			}
			
			else
			{
				Intent intent = new Intent(this, MainActivity.class);
				startActivity(intent);
			}
			
			saveUserData();
		}
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
        	textView.setText(result);
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
}