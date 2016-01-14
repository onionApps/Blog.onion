package onion.blog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.IOException;

public class PostActivity extends AppCompatActivity {

    String id;
    EditText title;
    EditText content;
    ImageView image;
    Bitmap bitmap;
    Blog blog;
    int REQUEST_PICKER = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_post);

        blog = Blog.getInstance(this);

        title = (EditText) findViewById(R.id.title);
        content = (EditText) findViewById(R.id.content);
        image = (ImageView) findViewById(R.id.image);

        /*
        Intent intent = getIntent();

        title.setText(intent.getExtras().getString("title", ""));
        content.setText(intent.getExtras().getString("content", ""));

        Bitmap bmp = intent.get
        */

        id = getIntent().getStringExtra("id");

        if (id != null) {

            Blog.PostInfo postInfo = blog.getPostInfo(id);

            title.setText(postInfo.getTitle());
            content.setText(postInfo.getContent());

        }


        getSupportActionBar().setTitle(id != null ? "Edit" : "Create");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post, menu);
        menu.findItem(R.id.action_photo).setVisible(id == null);
        //menu.findItem(R.id.action_photo).setEnabled(id == null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_publish) {

            if (id != null) {
                blog.updatePost(id, title.getText().toString(), content.getText().toString());
            } else {

                blog.addPost(title.getText().toString(), content.getText().toString(), bitmap);
            }

            finish();
            return true;

        }

        if (item.getItemId() == R.id.action_photo) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICKER);
            return true;
        }

        return super.onOptionsItemSelected(item);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if (resultCode != RESULT_OK)
        //    return;
        if (requestCode == REQUEST_PICKER) {
            bitmap = null;
            if (resultCode == RESULT_OK) {
                try {
                    Uri uri = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                } catch (IOException ex) {
                    //Snackbar.make(title, "Error", Snackbar.LENGTH_SHORT).show();
                }
            }
            image.setImageBitmap(bitmap);
        }
    }

}
