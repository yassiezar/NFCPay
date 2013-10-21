package com.skripsie.nfcpay6;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ChangeSettings extends Activity implements OnClickListener
{
	private Button nextScreen;
	
	private EditText nameText;
	private EditText passText;
	
	private SQLiteDatabase myDatabase;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_change_settings);
		
		//deleteDatabase();
		
		nextScreen = (Button)findViewById(R.id.next_screen_button);
		nextScreen.setOnClickListener(this);
		
		nameText = (EditText)findViewById(R.id.name_text_change);
		passText = (EditText)findViewById(R.id.password_text_change);
	}
	
	public void onClick(View view)
	{
		if(view.getId() == R.id.next_screen_button)
		{
			deleteDatabase();
			saveData();
			
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			
			finish();
		}
	}
	
	public boolean saveData()
	{
		String TableName = "userTable";
		int userAge = 0;
		String userName = null;
		String userPass = null;
		
		try
		{
			myDatabase = this.openOrCreateDatabase("UserDatabase", MODE_PRIVATE, null);
			
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
			
			myDatabase.execSQL("INSERT INTO "
		     + TableName
		     + " (NameField, PassField)"
		     + " VALUES ('" + userName + "', '" + userPass + "');");
			
			((GlobalVar) this.getApplication()).setUserName(userName);
			((GlobalVar) this.getApplication()).setUserPassword(userPass);
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
	
	public void deleteDatabase()
	{
		myDatabase = this.openOrCreateDatabase("UserDatabase", MODE_PRIVATE, null);
		
		myDatabase.delete("userTable", null, null);
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
			
			Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
		}
		
		catch(Exception e)
		{
			Toast.makeText(this, "Could not read data", Toast.LENGTH_SHORT).show();
		}
		
		finally
		{
			
		}
	}

}
