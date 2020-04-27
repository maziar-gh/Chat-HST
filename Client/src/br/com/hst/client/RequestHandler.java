package br.com.hst.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import br.com.hst.client.constants.MenuConstants;

public class RequestHandler {

	private Socket client;
	private Scanner sc;
	private List<String> listUsers = new ArrayList<String>();

	public RequestHandler(Socket client) {
		this.client = client;
	}

	protected synchronized void startReceiveThread(DataInputStream dis, DataOutputStream dos) {
		Thread t = new Thread(new Runnable() {
			@Override
			synchronized public void run() {
				while (true) {
					try {
						String opcao = dis.readUTF();
						switch (opcao) {
						case MenuConstants.MENU:
							showMenu();
							break;
						case MenuConstants.LIST_USERS:
							receiveUsersList(dis);
							break;
						case MenuConstants.RECEIVE_MESSAGE:
							receiveMessage(dis);
							showMenu();
							break;
						case MenuConstants.RECEIVE_FILE:
							receiveFile(dis);
							showMenu();
							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			private void showMenu() {
				System.out.println("\n==============================\n1 - Visualizar usu�rios online\r\n" + 
							"2 - Enviar mensagem\r\n" +
							"3 - Enviar arquivos\r\n" + 
							"4 - Sair\n");
				System.out.println("> Escolha uma opera��o: ");
			}
		});
		t.start();
	}

	protected synchronized void startInputThread(DataInputStream dis, DataOutputStream dos, String name) {

		Thread t = new Thread(new Runnable() {

			@Override
			synchronized public void run() {
				while (true) {
					try {
						sc = new Scanner(System.in);
						String option = sc.nextLine();
						dos.writeUTF(option);
						switch (option) {
						case MenuConstants.SEND_MESSAGE:
							sendMessage(dos, name);
							break;
						case MenuConstants.SEND_FILE:
							sendFile(dos, name);
							break;
						case MenuConstants.EXIT:
							closeConnection();
							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		t.start();
	}

	private void sendMessage(DataOutputStream dos, String name) {
		try {
			Scanner sc = new Scanner(System.in);
			Thread.sleep(100);
			String target = userTarget(name, sc);
			dos.writeUTF(target);

			String message = sc.nextLine();
			System.out.print("> Para " + target + ": ");
			
			message = sc.nextLine();
			
			if (!message.isBlank()) {
				dos.writeUTF(message);
			}
		} catch (IOException e) {
			System.out.println("Falha ao enviar mensagem.");
		} catch (InterruptedException e) {
			System.out.println("Falha ao listar usu�rios.");
		}
		listUsers.clear();
	}

	private void sendFile(DataOutputStream dos, String name) {
		FileInputStream fis = null;
		try {
			sc = new Scanner(System.in);
			Thread.sleep(100);

			String target = userTarget(name, sc);
			dos.writeUTF(target);

			System.out.println("Digite o nome do Arquivo: ");
			String filename = sc.next();

			fis = new FileInputStream(new File(filename));
			int fileSize = fis.available();

			dos.writeUTF(filename);
			dos.writeInt(fileSize);

			int bufferSize = fileSize < 4096 ? fileSize : 4096;
			byte[] buffer = new byte[bufferSize];

			int remaining = fileSize;
			while (remaining > 0) {
				fis.read(buffer);
				if (remaining < bufferSize) {
					byte[] bufferOld = buffer;
					buffer = new byte[remaining];
					System.arraycopy(bufferOld, 0, buffer, 0, buffer.length);
				}
				dos.write(buffer);
				remaining -= buffer.length;
			}
			System.out.println("\nArquivo " + filename + " enviado para " + target);
		} catch (IOException e) {
			System.out.println("Falha ao enviar arquivo.");
		} catch (InterruptedException e) {
			System.out.println("Falha ao listar usu�rios.");
		}
		listUsers.clear();
	}

	private void receiveFile(DataInputStream dis) {
		FileOutputStream fos = null;
		try {
			String filename = dis.readUTF();
			System.out.println("\n\nArquivo recebido: " + filename);
			int fileSize = dis.readInt();
			System.out.println("Tamanho do arquivo: " + fileSize + " bytes\n");

			Path path = Paths.get("out" + File.separator + filename);
			if (!Files.exists(path.getParent())) {
				Files.createDirectory(path.getParent());
				Files.createFile(path);
			}

			fos = new FileOutputStream(path.toFile());

			int bufferSize = fileSize < 4096 ? fileSize : 4096;
			byte[] buffer = new byte[bufferSize];
			int remaining = fileSize;
			while (remaining > 0) {
				dis.read(buffer);
				if (remaining < bufferSize) {
					byte[] bufferOld = buffer;
					buffer = new byte[remaining];
					System.arraycopy(bufferOld, 0, buffer, 0, buffer.length);
				}
				fos.write(buffer);
				remaining -= buffer.length;
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Falha ao obter arquivo");
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
	}

	protected void closeConnection() throws IOException {
		System.out.println("Encerrando conex�o...");
		client.close();
		System.exit(0);
	}

	protected void receiveMessage(DataInputStream dis) {
		try {
			String message = dis.readUTF();
			System.out.println(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void receiveUsersList(DataInputStream dis) throws IOException {
		int num = dis.readInt();
		System.out.println("\nN�mero de usu�rios online: " + num);
		for (int i = 0; i < num; i++) {
			String user = dis.readUTF();
			listUsers.add(user);
			System.out.println(user);
		}
	}

	private String userTarget(String name, Scanner sc) {
		System.out.println("\nInforme o Destinat�rio: ");
		while (true) {
			String target = sc.next();

			if (!listUsers.contains(target)) {
				System.err.println("Destinat�rio inv�lido, digite novamente:");
				continue;
			}
			if (target.equals(name)) {
				System.err.println("Voc� digitou o pr�prio nome, digite um destinat�rio v�lido:");
				continue;
			}

			return target;
		}
	}
}