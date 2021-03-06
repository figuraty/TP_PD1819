
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Client_Management extends java.util.Observable {
    
    protected String username;
    public static final int TIMEOUT = 10; //segundos
    public static final String IP = "10.8.10.111";
    public static String DOWNLOAD_PATH;
    public static final String SAVE_PATH = "C:\\\\Users\\\\franc\\\\Desktop\\\\teste_1";
    public static final int TCP_PORT = 5001;
    public static final int UDP_PORT = 6001; 
    protected static Socket socket;
    protected static ObjectInputStream in = null;
    protected static ObjectOutputStream out = null;
    protected static ServerSocket serverSocket;
    protected static ServidorInterface servidorRMI = null;
    public static final int TRANSFER_PORT = 4002;
    public static byte[] file = new byte[4000];
    public static InputStream input;
    public static int nbytes;
    public static FileOutputStream localFileOutputStream = null;
    public static String localFilePath = null;
    public static File localDirectory;

    public Client_Management() {
        try {
            socket = new Socket(InetAddress.getByName(IP), TCP_PORT);
            socket.setSoTimeout(TIMEOUT*1000);   
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.out.println("Erro contrutor Client Management");
        }
    }
    
    public Pedido_Registo preencherDados(Pedido_Registo p){    
        try{
            out.writeObject(p);
            out.flush();
            p = (Pedido_Registo) in.readObject();
            return p;
        } catch (IOException e) {
            System.out.println("Erro preencher dados Client Management");
            return null;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Client_Management.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public Pedido_Utilizadores getUtilizadores(){
        
        try {
            Pedido_Utilizadores p = new Pedido_Utilizadores(username);
            p.setUtilizadores(servidorRMI.utilizadoresOnline(username));
//            out.writeObject(p);
//            out.flush();
//            p =  (Pedido_Utilizadores) in.readObject();
            return p;
        } catch (IOException e) {
            System.out.println("Erro get utilizadores Client Management");
            return null;
        }
    }
    
    public Pedido_Obter_Mensagens getMensagens(String user){
        try {
            Pedido_Obter_Mensagens p = new Pedido_Obter_Mensagens(username, user);
            out.writeObject(p);
            out.flush();
            p =  (Pedido_Obter_Mensagens) in.readObject();
            return p;
        } catch (IOException e) {
            System.out.println("Erro get mensagens Client Management");
            return null;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public Pedido_Escrever_Mensagem escreverMensagem(Pedido_Escrever_Mensagem p){
        try{
            out.writeObject(p);
            out.flush();
            p = (Pedido_Escrever_Mensagem) in.readObject();
            return p;
        } catch (IOException e) {
            System.out.println("Erro escrever mensagem Client Management : " + e);
            return null;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Client_Management.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public Pedido_Obter_Ficheiros getFicheiros(String username){
        Pedido_Obter_Ficheiros pedido = new Pedido_Obter_Ficheiros(username);
        Pedido_Obter_IP p = new Pedido_Obter_IP(username);
        try{
            out.writeObject(p);
            out.flush();
            p = (Pedido_Obter_IP) in.readObject();
            pedido.setIp(p.getIP());
            pedido.setFicheiros(servidorRMI.getFicheiros(username));
            return pedido;
        } catch (IOException e) {
            System.out.println("Erro obter ficheiros Client Management");
            return null;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Client_Management.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
     
    public int TransferirFicheiros(String user, String ficheiro, String ip){
        try {
            Socket s = new Socket(InetAddress.getByName(ip), TRANSFER_PORT);
            ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
            o.writeObject(ficheiro);
            o.flush();
            input = s.getInputStream();
            localDirectory = new File(SAVE_PATH);
            
            if(!localDirectory.exists()){
                System.out.println("A directoria " + localDirectory + " nao existe!");
                return -1;
            }
        
            if(!localDirectory.isDirectory()){
                System.out.println("O caminho " + localDirectory + " nao se refere a uma directoria!");
                return -1;
            }
        
            if(!localDirectory.canWrite()){
                System.out.println("Sem permissoes de escrita na directoria " + localDirectory);
                return -1;
            }
                    
            localFilePath = localDirectory.getCanonicalPath()+File.separator+ficheiro;
            localFileOutputStream = new FileOutputStream(localFilePath);
            int vezes = 0;
             while((nbytes = input.read(file)) > 0){
                 vezes++;
                localFileOutputStream.write(file,0,nbytes);
             }
             
             if(vezes == 0)
                 return -1;
             
        } catch (UnknownHostException ex) {
                System.out.println("Erro na transferencia de ficheiro: " + ex);
                return -1;
        } catch (IOException ex) {
                System.out.println("Erro na transferencia de ficheiro: " + ex);
                return -1;
        }
        adicionaTransferenciaFicheiro(username, ficheiro, user);
        return 1;
    }
    
    public void adicionaTransferenciaFicheiro(String username, String ficheiro, String user) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Pedido_Registar_Transferencia p = new Pedido_Registar_Transferencia(username, ficheiro, user, sdf.format(Calendar.getInstance().getTime()));
            out.writeObject(p);
            out.flush();
        } catch (IOException ex) {
            System.out.println("Erro a registar transferencia: " + ex);
        }
    }
        
    public void atualizarFicheiro(String fileName, File file, String action){
        try {
            String tamanho;
            if(file.length() / 1024 + 1 < 1024)
                tamanho = (file.length() / 1024 + 1) + " KB";
            else
                tamanho = ((file.length() / 1024 + 1) / 1024 + 1) + " MB";
            Pedido_Alterar_Ficheiro p = new Pedido_Alterar_Ficheiro(username, fileName, action, tamanho);
            out.writeObject(p);
            out.flush();
        } catch (IOException e) {
            System.out.println("Erro atualizar ficheiros Client Management: " + e);
        }
    }
    
    public void login(String username, String path){
        this.username = username;
        DOWNLOAD_PATH = path;
        try {
            String objectUrl = "rmi://" + IP + "/ServidorRMI";
            servidorRMI = (ServidorInterface) Naming.lookup(objectUrl);
            socket = new Socket(InetAddress.getByName(IP), TCP_PORT);
            Thread t = new Client_Update_TCP(socket, this);
            t.setDaemon(true);
            t.start();
            t = new Client_Update_UDP("localhost", this);
            t.setDaemon(true);
            t.start();
            t = new Thread(
                new Runnable(){
                    public void run(){
                        ThreadPedidosFicheiro();
                    }
                }
            );
            t.setDaemon(true);
            t.start();
            Timer timer = new Timer();
            TimerTask task = new Dir_Watcher(path){
                @Override
                protected void onChange(File file, String action){
                    atualizarFicheiro(file.getName(), file, action);
                }
            };
            timer.schedule(task, new Date(), 1000);
        } catch (IOException e) {
            System.out.println("Erro login Client Management:" + e);
        } catch (NotBoundException ex) {
            Logger.getLogger(Client_Management.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void logout(){
        try {
            out.writeObject("logout");
            out.flush();
        } catch (IOException e) {
            System.out.println("Erro logout Client Management");
        }
        username = null;
        DOWNLOAD_PATH = null;
    }
    
    public void update(String update){
        setChanged();
        notifyObservers(update);    
    }
    
    public void ThreadPedidosFicheiro(){
        try {
            serverSocket = new ServerSocket(TRANSFER_PORT);
            System.out.println("Thread transferencia iniciada no porto:" + TRANSFER_PORT);
            while(!serverSocket.isClosed()){
                try{
                    Thread t = new Atende_Pedido_Ficheiro(serverSocket.accept(), DOWNLOAD_PATH);
                    t.setDaemon(true);
                    t.start();
                } catch (IOException ex) {
                    Logger.getLogger(Client_Management.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Client_Management.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    ArrayList<Pedido_Registar_Transferencia> getTransferencias(String User) {
        try {
            Pedido_Obter_Transferencias p = new Pedido_Obter_Transferencias(User);
            out.writeObject(p);
            out.flush();
            p = (Pedido_Obter_Transferencias) in.readObject();
            return p.getTransferencias();
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Erro a ler transferencias: " + ex);
            return null;
        }        
    }

    
    
}
