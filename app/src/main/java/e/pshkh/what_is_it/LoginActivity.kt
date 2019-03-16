package e.pshkh.what_is_it

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import e.pshkh.what_is_it.data_transfer_object.UserDTO
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    // 파이어베이스 계정 인증을 위한 변수
    var auth : FirebaseAuth? = null

    var firestore : FirebaseFirestore? = null

    // 구글 로그인 계정을 위한 변수
    var googleSignInClient : GoogleSignInClient? = null

    var GOOGLE_LOGIN_CODE = 9002

    // onCreate는 앱의 화면이 처음으로 생성됬을때 호출 되는 부분입니다. 즉 앱의 특정 화면이 처음 켜질때 호출이 됩니다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 이메일 계정 생성 및 로그인 버튼에 이벤트를 다는겁니다. (activity_login.xml 디자인을 참고하세요)
        email_login_button.setOnClickListener {
            createAndLoginEmail()
        }

        /*
        // 구글 계정으로 로그인 버튼에 이벤트를 다는겁니다.
        google_sign_in_button.setOnClickListener {
            googleLogin()
        }

        // 구글 로그인 옵션 설정
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,gso)
        */

    }

    // 이메일 계정 생성
    fun createAndLoginEmail(){
        if(email_edittext.text.isNullOrBlank()||password_edittext.text.isNullOrBlank()){
            Toast.makeText(applicationContext,"이메일과 비밀번호를 입력해 주세요.", Toast.LENGTH_LONG).show() //이메일 입력란, 비밀번호 입력란 둘 중 하나라도 비어있으면 진행이 되지 않고 Toast메세지를 띄우는 역할을 합니다.
        }else{
            auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){
                    // 계정 생성이 성공하면 메인 액티비티로 이동합니다.
                    // currentUser라는건 현재 회원가입 및 로그인을 시도하는 유저입니다. 즉 사용자이죠


                    //계정을 생성할때 새롭게 생성된 사용자의 uid, 이메일 아이디를 데이터베이스에 저장합니다.


                    // verification
                    if(auth?.currentUser?.uid.isNullOrBlank() || auth?.currentUser?.email.isNullOrBlank()){
                        Toast.makeText(this, "유효한 이메일 계정과 비밀번호를 입력해주세요!", Toast.LENGTH_LONG)
                    }else{
                        var userDTO = UserDTO()

                        userDTO.uid = auth?.currentUser?.uid
                        userDTO.userEmail = auth?.currentUser?.email
                        userDTO.timestamp = System.currentTimeMillis()

                        firestore?.collection("users")?.document(userDTO.userEmail!!)?.set(userDTO)

                        moveMainPage(auth?.currentUser)
                    }
                }else if(task.exception?.message.isNullOrEmpty()){
                    // 예외가 발생하면 메세지를 찍어주는 기능입니다.
                    Toast.makeText(this,task.exception?.message, Toast.LENGTH_LONG).show()
                    Toast.makeText(this,"예외", Toast.LENGTH_LONG).show()
                }else{
                    //회원가입 성공도 아니고 실패도 아니면 기존에 이미 있는 계정으로 로그인을 시키면 되겠죠
                    signinEmail()
                }
            }
        }

    }

    // 이메일 계정으로 로그인
    fun signinEmail(){
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())?.addOnCompleteListener {
                task ->
            if(task.isSuccessful){
                moveMainPage(auth?.currentUser)
            }else{
                Toast.makeText(this,task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // 메인 액티비티로 이동합니다.
    fun moveMainPage(user : FirebaseUser?){
        if(user != null){
            // 액티비티를 옮겨다닐때는 startActivity를 사용합니다.
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }

    /*
    // 구글 로그인
    fun googleLogin(){
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent,GOOGLE_LOGIN_CODE)
    }

    // 파이어베이스 구글 계정 인증을 처리하는 부분입니다.
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount){
        var credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            if(task.isSuccessful){

                /*
                    계정을 생성할때 새롭게 생성된 사용자의 uid, 이메일 아이디를 데이터베이스에 저장합니다.

                */
                var userDTO = UserDTO()

                userDTO.uid = auth?.currentUser?.uid
                userDTO.userEmail = auth?.currentUser?.email

                firestore?.collection("users")?.document(userDTO.userEmail!!)?.set(userDTO)

                System.out.println("구글 로그인 2단계")

                moveMainPage(auth?.currentUser)
            }
        }

    }
    */

    // onResume는 앱의 화면이 잠시 백그라운드로 가있다가 다시 켜지면 실행되는 곳입니다.
    // 앱을 사용하다가 홈버튼을 눌러서 밖으로 나갔다가 다시 들어올때 실행되는 곳이에요
    override fun onResume() {
        super.onResume()
        moveMainPage(auth?.currentUser) // 자동 로그인
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /*
        if(requestCode == GOOGLE_LOGIN_CODE){
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess){
                System.out.println("구글 로그인 1단계")
                var account = result.signInAccount
                System.out.println(account)
                firebaseAuthWithGoogle(account!!)
            }
        }
        */
    }


}


