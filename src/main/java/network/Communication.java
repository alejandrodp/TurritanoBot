package network;

import com.mongodb.client.model.Filters;
import model.DBController;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hello world!
 *
 */
public class Communication extends TelegramLongPollingBot {

    private boolean productsAmount = false;
    private boolean choosing = false;
    private boolean finalorder = false;
    private boolean ismaduros = true;
    private boolean isverdes = true;
    private int maduros = 0;
    private  int verdes = 0;
    private boolean chooseCuarto = false;
    private String edificio = "";
    private int cuarto = 0;


    @Override
    public void onUpdateReceived(Update update) {

        try {
            if(update.hasMessage() && update.getMessage().hasText()){
                proccessTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                proccessCallbackQuery(update);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Procesa los comandos y los envía a la clase correspondiente.
     * @param update Comando del/home/acnissarin/Projects/CEbot usuario.
     */
    private void proccessTextMessage(Update update) throws TelegramApiException{

        String message = update.getMessage().getText();

        if(message.equals("/start") | message.equals("/start@TheCEBot")){
            execute(startCommand(update.getMessage().getChatId()));

        } else if(message.equals("Ordenes pendientes")&& !productsAmount && !chooseCuarto){

            Bson filter = Filters.eq("id", update.getMessage().getChatId());

            List<Document> docs = DBController.getInstance().getDocs("pedidos", filter);

            if (docs.size() == 0) {
                SendMessage warning = new SendMessage();
                warning.setChatId(update.getMessage().getChatId());
                warning.setText("No tiene órdenes pendientes");
                execute(warning);
            } else {
                for (Document doc : docs){
                    SendMessage response = new SendMessage();
                    response.setChatId(update.getMessage().getChatId());

                    response.setText(doc.getString("numero") +
                            "\nLugar de entrega:\n" +
                            doc.getString("edificio") +
                            "\nHabitación: " + doc.getInteger("cuarto") +
                            "\nCantidad de productos maduros: " + doc.getInteger("maduros") +
                            "\nCantidad de productos verdes: " + doc.getInteger("verdes") +
                            "\nFecha de entrega: " + doc.getString("fecha_programada"));

                    DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                    String day = ZonedDateTime.now(ZoneId.of("America/Costa_Rica")).format(dayFormat);

                    if (!day.equals(doc.getString("fecha_programada"))){
                        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

                        List<List<InlineKeyboardButton>> buttons = new ArrayList<>(Collections.singletonList(new ArrayList<>(Collections.singleton(new InlineKeyboardButton("Cancelar pedido").setCallbackData(doc.getString("numero"))))));

                        keyboard.setKeyboard(buttons);
                        response.setReplyMarkup(keyboard);
                    }
                    execute(response);
                }

                SendMessage warning = new SendMessage();
                warning.setChatId(update.getMessage().getChatId());
                warning.setText("Recuerde que una vez llegado el día de la entrega ya no se podrá cancelar la orden");
                execute(warning);
            }


        } else if(message.equals("Comprar productos") && !productsAmount && !chooseCuarto){

            SendMessage response = new SendMessage();
            response.setChatId(update.getMessage().getChatId());
            if (ismaduros){
                response.setText("Digite la cantidad deseada de productos maduros");
            }
            execute(response);
            productsAmount = true;

        } else if(productsAmount && !choosing) {

            SendMessage response = new SendMessage();
            response.setChatId(update.getMessage().getChatId());
            try {

                if (ismaduros) {
                    maduros = Integer.parseInt(message);
                    response.setText("Digite la cantidad de productos verdes");
                    ismaduros = false;
                } else if (isverdes) {
                    verdes = Integer.parseInt(message);
                    response.setText("Cantidad de productos maduros: " + maduros + "\nCantidad de productos verdes: " + verdes + "\n\n¿Dónde desea recibir los productos?");
                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

                    List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                    buttons.add(new ArrayList<>(Collections.singleton(new InlineKeyboardButton("J1").setCallbackData("Edificio_J1"))));
                    buttons.add(new ArrayList<>(Collections.singleton(new InlineKeyboardButton("J2").setCallbackData("Edificio_J2"))));
                    buttons.add(new ArrayList<>(Collections.singleton(new InlineKeyboardButton("J3").setCallbackData("Edificio_J3"))));
                    buttons.add(new ArrayList<>(Collections.singleton(new InlineKeyboardButton("J4").setCallbackData("Edificio_J4"))));
                    buttons.add(new ArrayList<>(Collections.singleton(new InlineKeyboardButton("J5").setCallbackData("Edificio_J5"))));
                    buttons.add(new ArrayList<>(Collections.singleton(new InlineKeyboardButton("J6").setCallbackData("Edificio_J6"))));
                    buttons.add(new ArrayList<>(Collections.singleton(new InlineKeyboardButton("J7").setCallbackData("Edificio_J7"))));

                    keyboard.setKeyboard(buttons);
                    response.setReplyMarkup(keyboard);
                    isverdes = false;
                    choosing = true;
                }

            } catch (Exception e){
                response.setText("Error: introduzca un número entero");
            }

            execute(response);

        } else if(chooseCuarto && !finalorder){
            SendMessage response = new SendMessage();
            response.setChatId(update.getMessage().getChatId());
            try {
                cuarto = Integer.parseInt(message);
                response.setText("Cantidad de productos maduros: " + maduros + "\nCantidad de productos verdes: " + verdes + "\n" + edificio + "\nHabitación: " + cuarto + "\n\n¿Esto es correcto?");
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                buttons.add(new ArrayList<>(Collections.singleton(new InlineKeyboardButton("Sí").setCallbackData("Yes"))));
                buttons.add(new ArrayList<>(Collections.singleton(new InlineKeyboardButton("No").setCallbackData("Nein"))));

                keyboard.setKeyboard(buttons);
                response.setReplyMarkup(keyboard);
                finalorder = true;
            } catch (Exception e){
                response.setText("Error: introduzca un número entero");
            }

            execute(response);

        } else if (message.equals("Edificios ya recorridos")){

            SendMessage response = new SendMessage();
            response.setChatId(update.getMessage().getChatId());



            DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("EEEE");

            String day = ZonedDateTime.now(ZoneId.of("America/Costa_Rica")).format(dayFormat);

            DateTimeFormatter hourFormat = DateTimeFormatter.ofPattern("HH");

            int hour = Integer.parseInt(ZonedDateTime.now(ZoneId.of("America/Costa_Rica")).format(hourFormat));

            if((day.equals("Monday") && hour >= 20 && hour <= 22) || (day.equals("Friday") && hour >= 20 && hour <= 22)){
                List<Document> data = DBController.getInstance().getDocs("edificios_recorridos", null);
                StringBuilder sb = new StringBuilder();
                sb.append("Edificios ya recorridos\n");
                for (Document elm : data){
                    sb.append("\n").append(elm.getString("Edificio")).append(": ").append(elm.getString("recorrido"));
                }
                response.setText(sb.toString());
            } else {
                response.setText("Aun no es día de entrega");
            }
            execute(response);
        } else if(message.equals("Horarios de entrega")) {
            SendMessage response = new SendMessage();
            response.setChatId(update.getMessage().getChatId());
            response.setText("El horario de entrega es:" +
                    "\n\nLunes y viernes a partir de las 8 p.m y hasta las 10 p.m" +
                    "\nPedidos en viernes o lunes se realizan hasta el siguiente día de entrega");
            execute(response);
        } else {

            SendMessage response = new SendMessage().setChatId(update.getMessage().getChatId());
            //Logger.getGlobal().warning("Opción desconocida: " + update.getMessage().getText());
            response.setText("Error: opción desconocida (" + update.getMessage().getText() + ")");
            execute(response);
        }


    }

    /**
     * Procesa los comandos enviados desde un menú inline.
     * @param update Objeto con el CallbackQuery.
     */
    private void proccessCallbackQuery(Update update) throws TelegramApiException {

        String call_data = update.getCallbackQuery().getData();

        EditMessageText response = new EditMessageText();
        response.setChatId(update.getCallbackQuery().getMessage().getChatId());
        response.setMessageId(update.getCallbackQuery().getMessage().getMessageId());

        if (choosing) {
            switch (call_data) {
                case "Edificio_J1":
                    edificio = "Edificio: J1";
                    response.setText("Cantidad de productos maduros: " + maduros + "\nCantidad de productos verdes: " + verdes + "\n" + edificio + "\n\n¿En cual habitación?");
                    execute(response);
                    chooseCuarto = true;
                    break;
                case "Edificio_J2":
                    edificio = "Edificio: J2";
                    response.setText("Cantidad de productos maduros: " + maduros + "\nCantidad de productos verdes: " + verdes + "\n" + edificio + "\n\n¿En cual habitación?");
                    execute(response);
                    chooseCuarto = true;
                    break;
                case "Edificio_J3":
                    edificio = "Edificio: J3";
                    response.setText("Cantidad de productos maduros: " + maduros + "\nCantidad de productos verdes: " + verdes + "\n" + edificio + "\n\n¿En cual habitación?");
                    execute(response);
                    chooseCuarto = true;
                    break;
                case "Edificio_J4":
                    edificio = "Edificio: J4";
                    response.setText("Cantidad de productos maduros: " + maduros + "\nCantidad de productos verdes: " + verdes + "\n" + edificio + "\n\n¿En cual habitación?");
                    execute(response);
                    chooseCuarto = true;
                    break;
                case "Edificio_J5":
                    edificio = "Edificio: J5";
                    response.setText("Cantidad de productos maduros: " + maduros + "\nCantidad de productos verdes: " + verdes + "\n" + edificio + "\n\n¿En cual habitación?");
                    execute(response);
                    chooseCuarto = true;
                    break;
                case "Edificio_J6":
                    edificio = "Edificio: J6";
                    response.setText("Cantidad de productos maduros: " + maduros + "\nCantidad de productos verdes: " + verdes + "\n" + edificio +"\n\n¿En cual habitación?");
                    execute(response);
                    chooseCuarto = true;
                    break;
                case "Edificio_J7":
                    edificio = "Edificio: J7";
                    response.setText("Cantidad de productos maduros: " + maduros + "\nCantidad de productos verdes: " + verdes + "\n" + edificio +"\n\n¿En cual habitación?");
                    execute(response);
                    chooseCuarto = true;
                    break;

                case "Yes":
                    isverdes = true;
                    ismaduros = true;
                    chooseCuarto = false;
                    productsAmount = false;
                    finalorder = false;
                    choosing = false;

                    Bson filter = Filters.eq("id", update.getCallbackQuery().getMessage().getChatId());

                    int docs = DBController.getInstance().countDocuments("pedidos", filter);

                    DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE");

                    String day = ZonedDateTime.now(ZoneId.of("America/Costa_Rica")).format(format);

                    int daysPLus = 0;

                    if (day.equals("Monday")){
                        while (!day.equals("Friday")){
                            daysPLus++;
                            day = ZonedDateTime.now(ZoneId.of("America/Costa_Rica")).plusDays(daysPLus).format(format);
                        }
                    } else if (day.equals("Friday")){
                        while (!day.equals("Monday")){
                            daysPLus++;
                            day = ZonedDateTime.now(ZoneId.of("America/Costa_Rica")).plusDays(daysPLus).format(format);
                        }
                    } else {
                        while(!day.equals("Monday") && !day.equals("Friday")){
                            daysPLus++;
                            day = ZonedDateTime.now(ZoneId.of("America/Costa_Rica")).plusDays(daysPLus).format(format);
                        }
                    }

                    String fecha = ZonedDateTime.now(ZoneId.of("America/Costa_Rica")).plusDays(daysPLus).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                    List<Document> pedido = new ArrayList<>();
                    pedido.add(new Document()
                            .append("id", update.getCallbackQuery().getMessage().getChatId())
                            .append("numero", "Pedido#" + (docs + 1))
                            .append("edificio", edificio)
                            .append("cuarto", cuarto)
                            .append("maduros", maduros)
                            .append("verdes",  verdes)
                            .append("fecha_programada", fecha));
                    DBController.getInstance().saveDocs("pedidos", pedido);

                    response.setText("Bien, pedido realizado, la entrega se realizará en: \n" + fecha);
                    execute(response);

                    break;

                case "Nein":
                    response.setText("Ok, orden cancelada");
                    execute(response);
                    isverdes = true;
                    ismaduros = true;
                    chooseCuarto = false;
                    productsAmount = false;
                    finalorder = false;
                    choosing = false;
                    break;
                default:
                    long message_id = update.getCallbackQuery().getMessage().getMessageId();
                    long chat_id = update.getCallbackQuery().getMessage().getChatId();
                    execute(new EditMessageText()
                            .setChatId(chat_id)
                            .setMessageId((int) message_id)
                            .setText("Error: Comando no reconocido, por favor, intente otra vez."));
                    break;
            }
        } else if(call_data.contains("Pedido")){

            Bson filter = Filters.and(Filters.eq("id", update.getCallbackQuery().getMessage().getChatId()),
                    Filters.eq("numero", call_data));

            long deleted = DBController.getInstance().deleteDoc("pedidos", filter);

            if (deleted > 0){
                response.setText("Ok, pedido cancelado.");
                execute(response);
            } else {
                response.setText("Error, no se encontró la pedido, puede que ya haya sido eliminado o hubiera un error interno, intente de nuevo");
                execute(response);
            }


        } else{
            long message_id = update.getCallbackQuery().getMessage().getMessageId();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();
            execute(new EditMessageText()
                    .setChatId(chat_id)
                    .setMessageId((int) message_id)
                    .setText("Error en la orden, por favor haga el pedido nuevamente"));
        }


    }

    @Override
    public String getBotUsername() {
        return "turritano_bot";
    }

    @Override
    public String getBotToken() {
        return "969918690:AAH5czz4srmZx_i000oyL5Vd2u09nAHshf8";
    }

    /**
     * Responde al comando /start con el menú principal.
     * @param chatID ID del chat del solicitante.
     * @return Mensaje en respuesta al comando.
     */
    private SendMessage startCommand(long chatID){

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Comprar productos"));
        row1.add(new KeyboardButton("Edificios ya recorridos"));
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Horarios de entrega"));
        row2.add(new KeyboardButton("Ordenes pendientes"));
        rows.add(row2);

        keyboard.setKeyboard(rows);
        keyboard.setOneTimeKeyboard(true);

        SendMessage response = new SendMessage();

        response.setChatId(chatID);

        response.setText("Seleccione una opción:");
        response.setReplyMarkup(keyboard);
        return response;
    }

}
