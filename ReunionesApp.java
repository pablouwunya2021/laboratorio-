import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

enum TipoPlan {
    BASE,
    PREMIUM
}

enum EstadoReunion {
    DISPONIBLE,
    OCUPADO
}

class Usuario {
    private String usuario;
    private String contraseñaHash;
    private TipoPlan tipoPlan;
    private List<Reunion> reunionesProgramadas;

    public Usuario(String usuario, String contraseña, TipoPlan tipoPlan) {
        this.usuario = usuario;
        this.contraseñaHash = hashContraseña(contraseña);
        this.tipoPlan = tipoPlan;
        this.reunionesProgramadas = new ArrayList<>();
    }

    public void cambiarPlan(TipoPlan nuevoPlan) {
        this.tipoPlan = nuevoPlan;
    }

    public void cambiarContraseña(String nuevaContraseña) {
        this.contraseñaHash = hashContraseña(nuevaContraseña);
    }

    public void programarReunion(Reunion reunion) {
        if (this.tipoPlan == TipoPlan.BASE && reunionesProgramadas.size() >= 2) {
            System.out.println("Los usuarios base solo pueden reservar 2 reuniones al día.");
            return;
        }
        if (this.tipoPlan == TipoPlan.PREMIUM && reunionesProgramadas.size() >= 5) {
            System.out.println("Los usuarios premium tienen un límite de 5 reuniones al día.");
            return;
        }
        if (reunionesProgramadas.stream().anyMatch(r -> r.getMismaFechaHora(reunion))) {
            System.out.println("No es posible programar más de una reunión en la misma fecha y horario.");
            return;
        }
        reunionesProgramadas.add(reunion);
        System.out.println("Reunión programada con éxito.");
        guardarEnCSV("usuarios.csv", this);
    }

    public List<Reunion> listarReuniones() {
        return reunionesProgramadas;
    }

    public List<String> listarContactos() {
        return reunionesProgramadas.stream()
                .flatMap(reunion -> reunion.getListaInvitados().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public String getUsuario() {
        return usuario;
    }

    public TipoPlan getTipoPlan() {
        return tipoPlan;
    }

    String hashContraseña(String contraseña) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(contraseña.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al generar el hash de la contraseña.", e);
        }
    }

    void guardarEnCSV(String archivo, Usuario usuario) {
        List<Usuario> usuarios = cargarUsuariosDesdeCSV(archivo);
        usuarios.removeIf(u -> u.getUsuario().equals(usuario.getUsuario()));
        usuarios.add(usuario);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            for (Usuario u : usuarios) {
                writer.write(u.getUsuario() + "," + u.getContraseñaHash() + "," + u.getTipoPlan() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void guardarEnCSV(List<Usuario> usuarios, String archivo) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
            for (Usuario usuario : usuarios) {
                writer.write(usuario.getUsuario() + "," + usuario.getContraseñaHash() + "," + usuario.getTipoPlan() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Usuario> cargarUsuariosDesdeCSV(String archivo) {
        List<Usuario> usuarios = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    TipoPlan tipoPlan = TipoPlan.valueOf(parts[2]);
                    Usuario usuario = new Usuario(parts[0], parts[1], tipoPlan);
                    usuarios.add(usuario);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return usuarios;
    }

    public String getContraseñaHash() {
        return contraseñaHash;
    }
}

class Reunion {
    private LocalDate fecha;
    private LocalTime hora;
    private String nombre;
    private String pin;
    private String notas;
    private int duracion;
    private List<String> listaInvitados;
    private EstadoReunion estado;

    public Reunion(LocalDate fecha, LocalTime hora, String nombre, int duracion, Usuario organizador) {
        this.fecha = fecha;
        this.hora = hora;
        this.nombre = nombre;
        this.pin = generarPINUnico();
        this.duracion = duracion;
        this.listaInvitados = new ArrayList<>();
        this.estado = EstadoReunion.DISPONIBLE;
        listaInvitados.add(organizador.getUsuario());
    }

    private String generarPINUnico() {
        return "PIN-" + Math.round(Math.random() * 10000);
    }

    public boolean getMismaFechaHora(Reunion otraReunion) {
        return this.fecha.equals(otraReunion.getFecha()) && this.hora.equals(otraReunion.getHora());
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public List<String> getListaInvitados() {
        return listaInvitados;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    @Override
    public String toString() {
        return "Reunion{" +
                "fecha=" + fecha +
                ", hora=" + hora +
                ", nombre='" + nombre + '\'' +
                ", pin='" + pin + '\'' +
                ", notas='" + notas + '\'' +
                ", duracion=" + duracion +
                ", listaInvitados=" + listaInvitados +
                ", estado=" + estado +
                '}';
    }
}

public class ReunionesApp {
    private static final String USUARIOS_CSV = "usuarios.csv";

    public static void main(String[] args) {
        List<Usuario> usuarios = Usuario.cargarUsuariosDesdeCSV(USUARIOS_CSV);

        Usuario usuarioActual = iniciarSesion(usuarios);
        if (usuarioActual == null) {
            System.out.println("Inicio de sesión fallido. Saliendo del programa.");
            return;
        }

        int opcion;
        do {
            mostrarMenu();
            opcion = obtenerOpcion();
            procesarOpcion(opcion, usuarioActual, usuarios);
        } while (opcion != 6);

        System.out.println("Gracias por usar la aplicación. ¡Hasta luego!");
    }

    private static Usuario iniciarSesion(List<Usuario> usuarios) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bienvenido a la aplicación de reuniones en línea!");

        try {
            System.out.println("Seleccione una opción:");
            System.out.println("1. Iniciar sesión");
            System.out.println("2. Crear nuevo usuario");

            int seleccion = scanner.nextInt();
            scanner.nextLine();  // Consume la nueva línea después de nextInt

            switch (seleccion) {
                case 1:
                    System.out.print("Ingrese su usuario: ");
                    String usuario = scanner.nextLine();
                    System.out.print("Ingrese su contraseña: ");
                    String contraseña = scanner.nextLine();

                    for (Usuario u : usuarios) {
                        if (u.getUsuario().equals(usuario) && u.getContraseñaHash().equals(u.hashContraseña(contraseña))) {
                            System.out.println("Inicio de sesión exitoso. ¡Hola, " + usuario + "!");
                            return u;
                        }
                    }
                    System.out.println("Usuario o contraseña incorrectos.");
                    break;
                case 2:
                    Usuario nuevoUsuario = crearNuevoUsuario();
                    usuarios.add(nuevoUsuario);
                    System.out.println("Nuevo usuario creado exitosamente.");
                    Usuario.guardarEnCSV(usuarios, USUARIOS_CSV);
                    break;
                default:
                    System.out.println("Opción no válida. Saliendo del programa.");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }

        return null;
    }

    private static void mostrarMenu() {
        System.out.println("\n--- Menú ---");
        System.out.println("1. Programar Reunión");
        System.out.println("2. Listar Reuniones");
        System.out.println("3. Listar Contactos");
        System.out.println("4. Cambiar Plan");
        System.out.println("5. Cambiar Contraseña");
        System.out.println("6. Salir");
    }

    private static int obtenerOpcion() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese su opción: ");

        try {
            return scanner.nextInt();
        } catch (Exception e) {
            return 0;
        }
    }

    private static void procesarOpcion(int opcion, Usuario usuarioActual, List<Usuario> usuarios) {
        switch (opcion) {
            case 1:
                programarReunion(usuarioActual);
                break;
            case 2:
                listarReuniones(usuarioActual);
                break;
            case 3:
                listarContactos(usuarioActual);
                break;
            case 4:
                cambiarPlan(usuarioActual, usuarios);
                break;
            case 5:
                cambiarContraseña(usuarioActual);
                break;
            case 6:
                System.out.println("Saliendo del programa.");
                break;
            default:
                System.out.println("Opción no válida. Inténtelo de nuevo.");
                break;
        }
    }

    private static Usuario crearNuevoUsuario() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el nombre de usuario: ");
        String nuevoUsuario = scanner.nextLine();
        System.out.print("Ingrese la contraseña: ");
        String nuevaContraseña = scanner.nextLine();
        System.out.print("Seleccione el tipo de plan (BASE/PREMIUM): ");
        String tipoPlanString = scanner.nextLine();
        TipoPlan nuevoTipoPlan = TipoPlan.valueOf(tipoPlanString.toUpperCase());

        return new Usuario(nuevoUsuario, nuevaContraseña, nuevoTipoPlan);
    }

    private static void programarReunion(Usuario usuario) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.print("Ingrese la fecha de la reunión (AAAA-MM-DD): ");
            String fechaString = scanner.nextLine();
            LocalDate fecha = LocalDate.parse(fechaString);

            System.out.print("Ingrese la hora de la reunión (HH:MM): ");
            String horaString = scanner.nextLine();
            LocalTime hora = LocalTime.parse(horaString);

            System.out.print("Ingrese el nombre de la reunión: ");
            String nombre = scanner.nextLine();

            System.out.print("Ingrese la duración de la reunión (en minutos): ");
            int duracion = scanner.nextInt();
            scanner.nextLine(); // Consumir la nueva línea después de nextInt

            Reunion reunion = new Reunion(fecha, hora, nombre, duracion, usuario);

            System.out.print("¿Desea agregar notas a la reunión? (S/N): ");
            String agregarNotas = scanner.nextLine();
            if (agregarNotas.equalsIgnoreCase("S")) {
                System.out.print("Ingrese las notas de la reunión: ");
                String notas = scanner.nextLine();
                reunion.setNotas(notas);
            }

            usuario.programarReunion(reunion);
        } catch (Exception e) {
            System.out.println("Error al programar la reunión. Asegúrese de ingresar datos válidos.");
        } finally {
            scanner.close();
        }
    }

    private static void listarReuniones(Usuario usuario) {
        List<Reunion> reuniones = usuario.listarReuniones();
        System.out.println("Listado de reuniones:");
        for (Reunion reunion : reuniones) {
            System.out.println(reunion);
        }
    }

    private static void listarContactos(Usuario usuario) {
        List<String> contactos = usuario.listarContactos();
        System.out.println("Listado de contactos:");
        for (String contacto : contactos) {
            System.out.println(contacto);
        }
    }

    private static void cambiarPlan(Usuario usuario, List<Usuario> usuarios) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("Plan actual: " + usuario.getTipoPlan());
            System.out.print("Ingrese el nuevo plan (BASE/PREMIUM): ");
            String nuevoPlanString = scanner.nextLine();
            TipoPlan nuevoPlan = TipoPlan.valueOf(nuevoPlanString.toUpperCase());

            usuario.cambiarPlan(nuevoPlan);
            System.out.println("Plan cambiado exitosamente.");
            Usuario.guardarEnCSV(usuarios, USUARIOS_CSV);
        } catch (Exception e) {
            System.out.println("Error al cambiar el plan. Asegúrese de ingresar un plan válido.");
        } finally {
            scanner.close();
        }
    }

    private static void cambiarContraseña(Usuario usuario) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.print("Ingrese la nueva contraseña: ");
            String nuevaContraseña = scanner.nextLine();
            usuario.cambiarContraseña(nuevaContraseña);
            System.out.println("Contraseña cambiada exitosamente.");
            usuario.guardarEnCSV(USUARIOS_CSV, usuario);
        } catch (Exception e) {
            System.out.println("Error al cambiar la contraseña. Asegúrese de ingresar una contraseña válida.");
        } finally {
        scanner.close();
        }
        }
        }




