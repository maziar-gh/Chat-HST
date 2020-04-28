package br.com.hst.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import br.com.hst.server.constants.MenuConstants;

public class ClientHandler implements Runnable {
	private Map<String, ClientHandler> clientList;
	private Socket client;
	private String name;
	private DataInputStream dis;
	private DataOutputStream dos;

	public ClientHandler(Socket client, Map<String, ClientHandler> clientList, String name, DataInputStream dis,
			DataOutputStream dos) {
		this.client = client;
		this.clientList = clientList;
		this.name = name;
		this.dis = dis;
		this.dos = dos;
	}

	@Override
	public void run() {
		try {
			String opcao = "";
			while (!opcao.equals(MenuConstants.EXIT)) {
				dos.writeUTF(MenuConstants.MENU);
				opcao = dis.readUTF();
				switch (opcao) {
				case MenuConstants.LIST_USERS:
					listUser();
					break;
				case MenuConstants.SEND_MESSAGE:
					listUser();
					String targetMessage = dis.readUTF();
					sendMessage(targetMessage);
					break;
				case MenuConstants.SEND_FILE:
					listUser();
					String target = dis.readUTF();
					sendFile(target);
					break;
				case MenuConstants.EXIT:
					System.out.println("O cliente " + this.name + " fechou a conex�o.");
					client.close();
					clientList.remove(this.name);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void listUser() throws IOException {
		dos.writeUTF(MenuConstants.LIST_USERS);
		clientList = Server.getClientList();
		dos.writeInt(clientList.size());
		clientList.forEach((key, value) -> {
			try {
				dos.writeUTF(key);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private void sendFile(String target) {
		try {
			DataOutputStream targetClient = Server.getClientList().get(target).getDataOutputStream();
			targetClient.writeUTF(MenuConstants.RECEIVE_FILE);

			String filename = dis.readUTF();
			filename = filename.substring(filename.lastIndexOf("\\") + 1);
			int fileSize = dis.readInt();
			int bufferSize = fileSize < 4096 ? fileSize : 4096;

			targetClient.writeUTF("received_" + System.currentTimeMillis() + "_" + filename);
			targetClient.writeInt(fileSize);

			byte[] buffer = new byte[bufferSize];
			int remaining = fileSize;
			while (remaining > 0) {
				dis.read(buffer);
				if (remaining < bufferSize) {
					byte[] bufferOld = buffer;
					buffer = new byte[remaining];
					System.arraycopy(bufferOld, 0, buffer, 0, buffer.length);
				}
				targetClient.write(buffer);
				remaining -= buffer.length;
			}
			System.out.println("\nArquivo " + filename + " recebido e redirecionado para " + target);

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Falha ao ler request");
		}
	}

	public DataOutputStream getDataOutputStream() {
		return dos;
	}

	protected void sendMessage(String targetMessage) {
		try {
			DataOutputStream targetClient = Server.getClientList().get(targetMessage).getDataOutputStream();
			String message = dis.readUTF();

			SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
			String hora = dateFormat.format(new Date());

			targetClient.writeUTF(MenuConstants.RECEIVE_MESSAGE);
			targetClient.writeUTF("\n\nVoc� recebeu uma mensagem:\n" + name + " disse [" + hora + "]: " + message +"\n");

		} catch (IOException e) {
			System.out.println("Falha ao enviar mensagem");
		}
	}
}