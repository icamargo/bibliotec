package controle;

import DAO.EmprestimoDAO;
import DAO.PessoaDAO;
import DAO.ReservaDAO;
import entidade.Emprestimo;
import entidade.ItemPrototype;
import entidade.LivroPrototype;
import entidade.Reserva;
import entidade.UsuarioPrototype;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
/**
 *
 * @author Igor
 */
@ManagedBean (name = "controleReserva")
@SessionScoped
public class ControleReserva {
    
    private List reservas;
    private ItemPrototype item;
    private Reserva novaReserva;
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final EmprestimoDAO emprestimoDAO = new EmprestimoDAO();
    private final PessoaDAO pessoaDAO = new PessoaDAO();
    
    public void cancelaReserva(Reserva reserva){
        if(reserva.getStatusReserva().equals("Cancelada")){
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Reserva ja Cancelada!"));
        }
        else{
            reserva.setStatusReserva("Cancelada");
            reservaDAO.cancelaReserva(reserva);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Reserva Cancelada com Sucesso!"));           
        }
    }
    
    public void solicitarReserva(ItemPrototype item, UsuarioPrototype usuario) throws IOException{
        FacesContext context = FacesContext.getCurrentInstance();
        
        if(!(usuario.getSituacao().equals("Inadimplente"))){
            if (!(item.getStatus().equals("Inativo"))){
                if(!(this.existeReserva(usuario, item))){
                    this.item = item;
                    this.reservas = reservaDAO.getReservas(item.getNumeroCatalogo());
                    context.getExternalContext().redirect("reservasItem.xhtml");
                }
                else context.addMessage(null, new FacesMessage("Você possui uma reserva em aberto para este item!"));               
            }
            else context.addMessage(null, new FacesMessage("Item Inativo! Não pode ser reservado!"));          
        }
        else context.addMessage(null, new FacesMessage("Usuário Inadimplente! Regularize a situação para então solicitar uma reserva"));
    }
    
    public void criarReserva(UsuarioPrototype usuario) throws IOException {
        Calendar dataRetirada = Calendar.getInstance();
        Calendar dataReserva = Calendar.getInstance();
        Calendar dataDisponibilizacao = Calendar.getInstance();
        int numeroCatalogo;
        
        if (reservas!=null && reservas.size() > 0){          
            //pega a maior data de disponibilização e acrescenta um dia e o resultado sera a data de retirada da reserva
            dataRetirada.setTime(reservaDAO.getMaiorDataDisponibilizacao(item.getNumeroCatalogo()).getTime());
        }
        
        if(item.getStatus().equals("Emprestado")){
            Emprestimo emprestimo;
            emprestimo = emprestimoDAO.getEmprestimo((LivroPrototype) item);
            dataRetirada = emprestimo.getDataDevPrevista();
            dataRetirada.add(Calendar.DAY_OF_MONTH, 1);
        }
        else{
            dataRetirada.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        dataDisponibilizacao.setTime(dataRetirada.getTime());
        dataDisponibilizacao.add(Calendar.DAY_OF_MONTH, 10);
        
        numeroCatalogo = item.getNumeroCatalogo();
        
        novaReserva = new Reserva(dataRetirada, dataDisponibilizacao, usuario.getCodigo(), dataReserva, numeroCatalogo, "Aberta");
        
        reservaDAO.novaReserva(novaReserva);
        usuario.getReservas().add(novaReserva);
        pessoaDAO.atualizarPessoa(usuario);

        DateFormat formataData = DateFormat.getDateInstance();
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage("Reserva Criada com Sucesso! Data de Retirada do Item: " + formataData.format(dataRetirada.getTime())));

    }
    
    public void verificaReservas(){
        List<Reserva> todasReservas;
        Calendar dataHoje = Calendar.getInstance();
        todasReservas = reservaDAO.todasReservas();
        for(Reserva reserva: todasReservas){
            if(reserva.getDataRetirada().compareTo(dataHoje) == -1){
                reserva.setStatusReserva("Cancelada");
                reservaDAO.cancelaReserva(reserva);
            }           
        }
    }

    public boolean existeReserva(UsuarioPrototype usuario, ItemPrototype item){
        return reservaDAO.buscaReservaExistente(usuario.getCodigo(), item.getNumeroCatalogo()) != null;
    }

    public List getReservas() {
        return reservas;
    }

    public void setReservas(List reservas) {
        this.reservas = reservas;
    }

    public ItemPrototype getItem() {
        return item;
    }

    public void setItem(ItemPrototype item) {
        this.item = item;
    }

}
